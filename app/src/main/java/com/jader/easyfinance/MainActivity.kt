// importante importar --> import androidx.compose.runtime.getValue
//importante importar --> import androidx.navigation.compose.rememberNavController


package com.jader.easyfinance

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.compose.runtime.Composable
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
import com.jader.easyfinance.ui.screens.RecurringTransactionsScreen
import com.jader.easyfinance.ui.screens.TransactionsScreen
import com.jader.easyfinance.ui.theme.EasyFinanceTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos de almacenamiento para versiones < Android 13
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                101
            )
        }
        // Solicitar permiso de notificaciones para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "easyfinance-db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "recurring-transaction-work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            EasyFinanceTheme {
                AppNavigation(db.transactionDao())
            }
        }
    }
}

@Composable
fun AppNavigation(transactionDao: com.jader.easyfinance.data.TransactionDao) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController, transactionDao = transactionDao)
        }
        composable("add_transaction") {
            AddTransactionScreen(navController = navController, transactionDao = transactionDao)
        }
        composable("add_transaction/{transactionId}") { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toIntOrNull()
            AddTransactionScreen(navController = navController, transactionDao = transactionDao, transactionId = transactionId)
        }
        composable("transactions") {
            TransactionsScreen(navController = navController, transactionDao = transactionDao)
        }
        composable("recurring_transactions") {
            RecurringTransactionsScreen(navController = navController, transactionDao = transactionDao)
        }
    }
}