package com.jader.easyfinance.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
        val db = Firebase.firestore

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "recurring_transaction_channel",
                "Transacciones Recurrentes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones para transacciones recurrentes generadas"
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val templates = db.collection("users").document(userId).collection("recurring_templates")
            .get().await().toObjects(RecurringTransactionTemplate::class.java)
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val currentTime = calendar.timeInMillis
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        val decimalFormat = DecimalFormat("C$ #,##0.00")

        templates.forEach { template ->
            template.startDate?.let { startDate ->
                Log.d("RecurringDebug", "Processing template: ${template.category}, StartDate: ${dateFormat.format(Date(startDate))}")
                if (currentTime >= startDate) {
                    val interval = when (template.recurrenceType) {
                        "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
                        "BIWEEKLY" -> 15 * 24 * 60 * 60 * 1000L
                        "MONTHLY" -> 30 * 24 * 60 * 60 * 1000L
                        else -> return@let
                    }
                    val elapsedTime = currentTime - startDate
                    val periodsElapsed = (elapsedTime / interval).toInt()
                    val lastPeriodStart = startDate + periodsElapsed * interval
                    if (currentTime >= lastPeriodStart && currentTime < lastPeriodStart + 24 * 60 * 60 * 1000L) {
                        val newTransactionDate = Calendar.getInstance(TimeZone.getDefault()).apply {
                            timeInMillis = lastPeriodStart
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        Log.d("RecurringDebug", "New transaction date: ${dateFormat.format(Date(newTransactionDate))}")

                        val transactionExists = db.collection("users").document(userId).collection("transactions")
                            .whereEqualTo("startDate", newTransactionDate)
                            .whereEqualTo("category", template.category)
                            .whereEqualTo("recurrenceType", template.recurrenceType)
                            .get().await().isEmpty.not()
                        if (!transactionExists) {
                            val transactionId = db.collection("users").document(userId).collection("transactions").document().id
                            db.collection("users").document(userId).collection("transactions").document(transactionId)
                                .set(
                                    Transaction(
                                        id = transactionId,
                                        amount = template.amount,
                                        category = template.category,
                                        isIncome = template.isIncome,
                                        isRecurring = template.isRecurring,
                                        recurrenceType = template.recurrenceType,
                                        startDate = newTransactionDate
                                    )
                                ).await()
                            val notification = NotificationCompat.Builder(applicationContext, "recurring_transaction_channel")
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("Transacción Recurrente Generada")
                                .setContentText("${if (template.isIncome) "Ingreso" else "Gasto"}: ${decimalFormat.format(template.amount)} - ${template.category}")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setAutoCancel(true)
                                .build()
                            NotificationManagerCompat.from(applicationContext).notify(newTransactionDate.toInt(), notification)
                            Log.d("RecurringDebug", "Inserted transaction: ${template.category}")

                            if (template.isIncome && template.category == "Sueldo") {
                                val taxExists = db.collection("users").document(userId).collection("transactions")
                                    .whereEqualTo("startDate", newTransactionDate)
                                    .whereEqualTo("category", "Impuestos")
                                    .whereEqualTo("recurrenceType", template.recurrenceType)
                                    .get().await().isEmpty.not()
                                if (!taxExists) {
                                    val inss = template.amount * 0.07
                                    val periodsPerYear = when (template.recurrenceType) {
                                        "WEEKLY" -> 52.0
                                        "BIWEEKLY" -> 24.0
                                        "MONTHLY" -> 12.0
                                        else -> 1.0
                                    }
                                    val annualGrossSalary = template.amount * periodsPerYear
                                    val annualNetSalary = annualGrossSalary - (inss * periodsPerYear)
                                    val annualIR = when {
                                        annualNetSalary <= 100_000 -> 0.0
                                        annualNetSalary <= 200_000 -> (annualNetSalary - 100_000) * 0.15
                                        annualNetSalary <= 350_000 -> (annualNetSalary - 200_000) * 0.20 + 15_000
                                        annualNetSalary <= 500_000 -> (annualNetSalary - 350_000) * 0.25 + 45_000
                                        else -> (annualNetSalary - 500_000) * 0.30 + 82_500
                                    }
                                    val irPerPeriod = annualIR / periodsPerYear
                                    val totalTax = inss + irPerPeriod
                                    val taxTransactionId = db.collection("users").document(userId).collection("transactions").document().id
                                    db.collection("users").document(userId).collection("transactions").document(taxTransactionId)
                                        .set(
                                            Transaction(
                                                id = taxTransactionId,
                                                amount = totalTax,
                                                category = "Impuestos",
                                                isIncome = false,
                                                isRecurring = template.isRecurring,
                                                recurrenceType = template.recurrenceType,
                                                startDate = newTransactionDate
                                    )
                                    ).await()
                                    val taxNotification = NotificationCompat.Builder(applicationContext, "recurring_transaction_channel")
                                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                                        .setContentTitle("Transacción de Impuestos Generada")
                                        .setContentText("Impuestos: ${decimalFormat.format(totalTax)}")
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                        .setAutoCancel(true)
                                        .build()
                                    NotificationManagerCompat.from(applicationContext).notify(newTransactionDate.toInt() + 1, taxNotification)
                                    Log.d("RecurringTax", "Inserted tax transaction: Impuestos")
                                }
                            }
                        }
                    }
                }
            }
        }
        return Result.success()
    }
}