package com.jader.easyfinance.data

import com.google.firebase.firestore.PropertyName

data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    @PropertyName("income") val isIncome: Boolean = false,
    @PropertyName("isRecurring") val isRecurring: Boolean = false,
    val recurrenceType: String? = null,
    val startDate: Long? = null
)