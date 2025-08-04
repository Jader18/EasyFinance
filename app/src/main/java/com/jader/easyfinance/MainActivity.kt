package com.jader.easyfinance

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jader.easyfinance.ui.screens.AddTransactionScreen
import com.jader.easyfinance.ui.screens.ChartsScreen
import com.jader.easyfinance.ui.screens.HomeScreen
import com.jader.easyfinance.ui.screens.LoginScreen
import com.jader.easyfinance.ui.screens.RecurringTransactionsScreen
import com.jader.easyfinance.ui.screens.TransactionsScreen
import com.jader.easyfinance.ui.theme.EasyFinanceTheme
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jader.easyfinance.data.RecurringTransactionWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Firestore offline persistence
        Firebase.firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        // Request permissions
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        // Schedule recurring transaction worker
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
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("add_transaction") {
            AddTransactionScreen(navController = navController)
        }
        composable("add_transaction/{transactionId}") { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")
            AddTransactionScreen(navController = navController, transactionId = transactionId)
        }
        composable("transactions") {
            TransactionsScreen(navController = navController)
        }
        composable("recurring_transactions") {
            RecurringTransactionsScreen(navController = navController)
        }
        composable("add_recurring_transaction/{templateId}") { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId")
            AddTransactionScreen(navController = navController, transactionId = templateId, isTemplate = true)
        }
        composable("charts") {
            ChartsScreen(navController = navController)
        }

    }
}