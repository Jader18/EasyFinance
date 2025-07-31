package com.jader.easyfinance.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class, RecurringTransactionTemplate::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_transaction_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `isIncome` INTEGER NOT NULL,
                        `isRecurring` INTEGER NOT NULL,
                        `recurrenceType` TEXT,
                        `startDate` INTEGER
                    )
                """)
                database.execSQL("""
                    INSERT INTO recurring_transaction_templates (id, amount, category, isIncome, isRecurring, recurrenceType, startDate)
                    SELECT id, amount, category, isIncome, isRecurring, recurrenceType, startDate
                    FROM transactions
                    WHERE isRecurring = 1
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE transactions_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        isIncome INTEGER NOT NULL,
                        isRecurring INTEGER NOT NULL,
                        recurrenceType TEXT,
                        startDate INTEGER,
                        UNIQUE(startDate, category, recurrenceType)
                    )
                """)
                database.execSQL("""
                    INSERT INTO transactions_temp (id, amount, category, isIncome, isRecurring, recurrenceType, startDate)
                    SELECT MIN(id), amount, category, isIncome, isRecurring, recurrenceType, startDate
                    FROM transactions
                    GROUP BY startDate, category, recurrenceType
                """)
                database.execSQL("DROP TABLE transactions")
                database.execSQL("ALTER TABLE transactions_temp RENAME TO transactions")
            }
        }
    }
}