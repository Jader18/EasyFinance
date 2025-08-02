package com.jader.easyfinance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplate(template: RecurringTransactionTemplate)

    @Update
    suspend fun update(transaction: Transaction)

    @Update
    suspend fun updateTemplate(template: RecurringTransactionTemplate)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Delete
    suspend fun deleteTemplate(template: RecurringTransactionTemplate)

    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM recurring_transaction_templates WHERE isRecurring = 1")
    fun getRecurringTemplates(): Flow<List<RecurringTransactionTemplate>>

    @Query("SELECT * FROM transactions WHERE isRecurring = 1")
    fun getRecurringTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isIncome = 0 AND category = 'Impuestos' AND startDate = :startDate AND recurrenceType = :recurrenceType")
    suspend fun getTaxTransaction(startDate: Long, recurrenceType: String?): Transaction?

    @Query("SELECT * FROM recurring_transaction_templates WHERE isIncome = 0 AND category = 'Impuestos' AND startDate = :startDate AND recurrenceType = :recurrenceType")
    suspend fun getTaxTemplate(startDate: Long, recurrenceType: String?): RecurringTransactionTemplate?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM transactions 
            WHERE (startDate = :startDate AND category = :category AND (recurrenceType = :recurrenceType OR recurrenceType IS NULL))
            AND (isRecurring = CASE WHEN :recurrenceType IS NULL THEN 0 ELSE 1 END)
        )
    """)
    suspend fun existsByStartDateAndCategoryAndRecurrenceType(
        startDate: Long,
        category: String,
        recurrenceType: String?
    ): Boolean
}
