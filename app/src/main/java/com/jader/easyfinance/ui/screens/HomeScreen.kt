package com.jader.easyfinance.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.jader.easyfinance.data.DataManager
import com.jader.easyfinance.data.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = Firebase.firestore
    val transactionsFlow: Flow<List<Transaction>> = callbackFlow {
        val listener = db.collection("users").document(userId)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Error fetching transactions: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val transactions = it.toObjects<Transaction>()
                    Log.d("FirestoreFetch", "HomeScreen fetched transactions: $transactions")
                    trySend(transactions).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }
    val transactions: State<List<Transaction>> = transactionsFlow.collectAsState(initial = emptyList())
    val totalIncomes: Double = transactions.value
        .filter { it.isIncome }
        .sumOf { it.amount }
    val totalExpenses: Double = transactions.value
        .filter { !it.isIncome }
        .sumOf { it.amount }
    val balance: Double = totalIncomes - totalExpenses
    val decimalFormat = DecimalFormat("C$ #,##0.00")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mimeTypes = arrayOf("text/*")

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            coroutineScope.launch {
                DataManager.exportToCsv(context, it)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            coroutineScope.launch {
                DataManager.importFromCsv(context, it)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Easy Finance") }
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight()
            ) {
                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Agregar Transacción") },
                    label = { Text("Agregar Transacción") },
                    selected = false,
                    onClick = { navController.navigate("add_transaction") }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = "Ver Transacciones") },
                    label = { Text("Ver Transacciones") },
                    selected = false,
                    onClick = { navController.navigate("transactions") }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = "Ver Transacciones Recurrentes") },
                    label = { Text("Transacciones Recurrentes") },
                    selected = false,
                    onClick = { navController.navigate("recurring_transactions") }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Filled.FileDownload, contentDescription = "Exportar Datos") },
                    label = { Text("Exportar Datos") },
                    selected = false,
                    onClick = { exportLauncher.launch("easyfinance_export_${System.currentTimeMillis()}.csv") }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Filled.FileUpload, contentDescription = "Importar Datos") },
                    label = { Text("Importar Datos") },
                    selected = false,
                    onClick = { importLauncher.launch(mimeTypes) }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "Gráficos") },
                    label = { Text("Gráficos") },
                    selected = false,
                    onClick = { navController.navigate("charts") }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Logout, contentDescription = "Cerrar Sesión") },
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Balance: ${decimalFormat.format(balance)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Ingresos: ${decimalFormat.format(totalIncomes)}",
                    fontSize = 18.sp
                )
                Text(
                    text = "Total Gastos: ${decimalFormat.format(totalExpenses)}",
                    fontSize = 18.sp
                )
            }
        }
    }
}