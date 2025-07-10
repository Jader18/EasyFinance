package com.jader.easyfinance

data class Transaction(
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val isIncome: Boolean
)