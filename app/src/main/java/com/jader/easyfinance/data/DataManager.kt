package com.jader.easyfinance.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

    suspend fun exportToCsv(context: Context, transactionDao: TransactionDao, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val transactions = transactionDao.getAllTransactions().first()
                val templates = transactionDao.getRecurringTemplates().first()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        // Escribir encabezados para transacciones
                        writer.write("=== Transactions ===\n")
                        writer.write("id,amount,category,isIncome,isRecurring,recurrenceType,startDate\n")
                        transactions.forEach { transaction ->
                            val startDate = transaction.startDate?.let { dateFormat.format(Date(it)) } ?: ""
                            writer.write("${transaction.id},${transaction.amount},${transaction.category},${transaction.isIncome},${transaction.isRecurring},${transaction.recurrenceType ?: ""},$startDate\n")
                        }
                        // Escribir encabezados para plantillas recurrentes
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

    suspend fun importFromCsv(context: Context, transactionDao: TransactionDao, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var isTransactionSection = false
                        var isTemplateSection = false
                        reader.lineSequence().toList().forEach { line ->
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
                                                val id = parts[0].toIntOrNull() ?: 0
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
                                                    if (id == 0 || !transactionDao.existsByStartDateAndCategoryAndRecurrenceType(
                                                            startDate ?: 0L, category, recurrenceType
                                                        )) {
                                                        transactionDao.insert(transaction)
                                                    } else {
                                                        transactionDao.update(transaction)
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
                                                    if (id == 0 || !transactionDao.existsByStartDateAndCategoryAndRecurrenceType(
                                                            startDate ?: 0L, category, recurrenceType
                                                        )) {
                                                        transactionDao.insertTemplate(template)
                                                    } else {
                                                        transactionDao.updateTemplate(template)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Ignorar líneas mal formadas
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
