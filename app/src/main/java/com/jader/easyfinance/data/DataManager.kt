package com.jader.easyfinance.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DataManager {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    suspend fun exportToCsv(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
                val db = Firebase.firestore
                val transactions = db.collection("users").document(userId).collection("transactions")
                    .get().await().toObjects(Transaction::class.java)
                val templates = db.collection("users").document(userId).collection("recurring_templates")
                    .get().await().toObjects(RecurringTransactionTemplate::class.java)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write("=== Transactions ===\n")
                        writer.write("id,amount,category,isIncome,isRecurring,recurrenceType,startDate\n")
                        transactions.forEach { transaction ->
                            val startDate = transaction.startDate?.let { dateFormat.format(Date(it)) } ?: ""
                            writer.write("${transaction.id},${transaction.amount},${transaction.category},${transaction.isIncome},${transaction.isRecurring},${transaction.recurrenceType ?: ""},$startDate\n")
                        }
                        writer.write("\n=== Recurring Templates ===\n")
                        writer.write("id,amount,category,isIncome,isRecurring,recurrenceType,startDate\n")
                        templates.forEach { template ->
                            val startDate = template.startDate?.let { dateFormat.format(Date(it)) } ?: ""
                            writer.write("${template.id},${template.amount},${template.category},${template.isIncome},${template.isRecurring},${template.recurrenceType ?: ""},$startDate\n")
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Datos exportados exitosamente", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    suspend fun importFromCsv(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
                val db = Firebase.firestore
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var isTransactionSection = false
                        var isTemplateSection = false

                        val existingTransactions = db.collection("users").document(userId).collection("transactions")
                            .get().await().toObjects(Transaction::class.java)
                        val existingTemplates = db.collection("users").document(userId).collection("recurring_templates")
                            .get().await().toObjects(RecurringTransactionTemplate::class.java)

                        reader.lineSequence().forEach { line ->
                            when (line.trim()) {
                                "=== Transactions ===" -> {
                                    isTransactionSection = true
                                    isTemplateSection = false
                                }
                                "=== Recurring Templates ===" -> {
                                    isTransactionSection = false
                                    isTemplateSection = true
                                }
                                else -> {
                                    if (line.isNotBlank() && !line.startsWith("id,amount")) {
                                        val parts = line.split(",")
                                        if (parts.size >= 7) {
                                            try {
                                                val id = parts[0]
                                                val amount = parts[1].toDoubleOrNull() ?: 0.0
                                                val category = parts[2]
                                                val isIncome = parts[3].toBoolean()
                                                val isRecurring = parts[4].toBoolean()
                                                val recurrenceType = parts[5].ifEmpty { null }
                                                val startDate = parts[6].ifEmpty { null }?.let {
                                                    dateFormat.parse(it)?.time
                                                }

                                                if (isTransactionSection) {
                                                    val transaction = Transaction(
                                                        id = id,
                                                        amount = amount,
                                                        category = category,
                                                        isIncome = isIncome,
                                                        isRecurring = isRecurring,
                                                        recurrenceType = recurrenceType,
                                                        startDate = startDate
                                                    )
                                                    if (id.isNotEmpty()) {
                                                        if (existingTransactions.none { it.id == id }) {
                                                            db.collection("users").document(userId).collection("transactions")
                                                                .document(id).set(transaction).await()
                                                        } else {
                                                            db.collection("users").document(userId).collection("transactions")
                                                                .document(id).update(
                                                                    mapOf(
                                                                        "amount" to amount,
                                                                        "category" to category,
                                                                        "isIncome" to isIncome,
                                                                        "isRecurring" to isRecurring,
                                                                        "recurrenceType" to recurrenceType,
                                                                        "startDate" to startDate
                                                                    )
                                                                ).await()
                                                        }
                                                    }
                                                } else if (isTemplateSection) {
                                                    val template = RecurringTransactionTemplate(
                                                        id = id,
                                                        amount = amount,
                                                        category = category,
                                                        isIncome = isIncome,
                                                        isRecurring = isRecurring,
                                                        recurrenceType = recurrenceType,
                                                        startDate = startDate
                                                    )
                                                    if (id.isNotEmpty()) {
                                                        if (existingTemplates.none { it.id == id }) {
                                                            db.collection("users").document(userId).collection("recurring_templates")
                                                                .document(id).set(template).await()
                                                        } else {
                                                            db.collection("users").document(userId).collection("recurring_templates")
                                                                .document(id).update(
                                                                    mapOf(
                                                                        "amount" to amount,
                                                                        "category" to category,
                                                                        "isIncome" to isIncome,
                                                                        "isRecurring" to isRecurring,
                                                                        "recurrenceType" to recurrenceType,
                                                                        "startDate" to startDate
                                                                    )
                                                                ).await()
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                // Ignore malformed line
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Datos importados exitosamente", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al importar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}