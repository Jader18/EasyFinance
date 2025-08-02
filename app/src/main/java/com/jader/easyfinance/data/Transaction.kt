package com.jader.easyfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String,
    val isIncome: Boolean,
    val isRecurring: Boolean = false,
    val recurrenceType: String? = null, // "WEEKLY", "BIWEEKLY", "MONTHLY"
    val startDate: Long? = null // Timestamp en milisegundos. Para transacciones regulares es fecha única, para recurrentes fecha de inicio
)
