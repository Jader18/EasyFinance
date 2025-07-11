package com.jader.easyfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jader.easyfinance.data.AppDatabase
import com.jader.easyfinance.data.RecurringTransactionWorker
import com.jader.easyfinance.ui.screens.AddTransactionScreen
import com.jader.easyfinance.ui.screens.HomeScreen
import com.jader.easyfinance.ui.screens.TransactionsScreen
import com.jader.easyfinance.ui.theme.EasyFinanceTheme
import java.util.concurrent.TimeUnit
// importante importar --> import androidx.compose.runtime.getValue
//importante importar --> import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Inicializar Room Database
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "easyfinance-db"
        ).addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        val transactionDao = db.transactionDao()

        // Programar WorkManager para transacciones recurrentes
        val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "recurring_transactions",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            EasyFinanceTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("home") {
                        HomeScreen(
                            onNavigateToAddTransaction = { navController.navigate("add_transaction") },
                            onNavigateToTransactions = { navController.navigate("transactions") },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable("add_transaction") {
                        AddTransactionScreen(
                            navController = navController,
                            transactionDao = transactionDao,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable("transactions") {
                        TransactionsScreen(
                            navController = navController,
                            transactionDao = transactionDao,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}