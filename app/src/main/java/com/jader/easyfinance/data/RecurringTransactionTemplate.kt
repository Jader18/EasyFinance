package com.jader.easyfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transaction_templates")
data class RecurringTransactionTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String,
    val isIncome: Boolean,
    val isRecurring: Boolean,
    val recurrenceType: String?,
    val startDate: Long?
)