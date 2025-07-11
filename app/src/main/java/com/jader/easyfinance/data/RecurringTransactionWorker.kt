package com.jader.easyfinance.data

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.Calendar

class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "easyfinance-db"
        ).build()
        val dao = db.transactionDao()

        val transactions = dao.getRecurringTransactions().first()
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        transactions.forEach { transaction ->
            transaction.startDate?.let { startDate ->
                if (currentTime >= startDate) {
                    val interval = when (transaction.recurrenceType) {
                        "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
                        "BIWEEKLY" -> 14 * 24 * 60 * 60 * 1000L
                        "MONTHLY" -> 30 * 24 * 60 * 60 * 1000L
                        else -> return@let
                    }
                    if ((currentTime - startDate) % interval <= 24 * 60 * 60 * 1000L) {
                        // Insertar transacción recurrente
                        dao.insert(
                            Transaction(
                                amount = transaction.amount,
                                category = transaction.category,
                                isIncome = transaction.isIncome,
                                isRecurring = transaction.isRecurring,
                                recurrenceType = transaction.recurrenceType,
                                startDate = startDate
                            )
                        )
                        // Si es Sueldo, calcular e insertar impuestos
                        if (transaction.isIncome && transaction.category == "Sueldo") {
                            // Calcular INSS (7%)
                            val inss = transaction.amount * 0.07
                            // Calcular IR
                            val periodsPerYear = when (transaction.recurrenceType) {
                                "WEEKLY" -> 52.0
                                "BIWEEKLY" -> 26.0
                                "MONTHLY" -> 12.0
                                else -> 1.0
                            }
                            val annualGrossSalary = transaction.amount * periodsPerYear
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
                            // Insertar transacción de impuestos
                            dao.insert(
                                Transaction(
                                    amount = totalTax,
                                    category = "Impuestos",
                                    isIncome = false,
                                    isRecurring = transaction.isRecurring,
                                    recurrenceType = transaction.recurrenceType,
                                    startDate = startDate
                                )
                            )
                        }
                    }
                }
            }
        }

        db.close()
        return Result.success()
    }
}