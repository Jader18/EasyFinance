package com.jader.easyfinance.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.jader.easyfinance.R
import com.jader.easyfinance.data.DataManager
import com.jader.easyfinance.data.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

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
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val totalIncomes: Double = transactions.value
        .filter { it.isIncome && it.startDate != null && Date(it.startDate) <= today.time }
        .sumOf { it.amount }
    val totalExpenses: Double = transactions.value
        .filter { !it.isIncome && it.startDate != null && Date(it.startDate) <= today.time }
        .sumOf { it.amount }
    val balance: Double = totalIncomes - totalExpenses
    val decimalFormat = DecimalFormat("C$ #,##0.00")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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

    // Lista simple de frases motivacionales
    val motivationalQuotes = listOf(
        "Cada centavo cuenta, ¡sigue adelante!",
        "Controla tus gastos, controla tu vida.",
        "El ahorro de hoy es la tranquilidad de mañana.",
        "Invierte en ti mismo y en tu futuro financiero.",
        "Una pequeña disciplina financiera puede cambiar tu vida.",
        "Gasta con conciencia, vive con libertad.",
        "Tus finanzas, tu poder.",
        "No es cuánto ganas, sino cuánto ahorras.",
        "El éxito financiero comienza con un buen hábito.",
        "Planea hoy para un mejor mañana."
    )

    // Escoge una frase aleatoria cada vez que el HomeScreen se componga
    val quote = remember { motivationalQuotes.random() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "MENU",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Agregar Transacción") },
                    label = { Text("Agregar Transacción") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("add_transaction")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = "Ver Transacciones") },
                    label = { Text("Ver Transacciones") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("transactions")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = "Ver Transacciones Recurrentes") },
                    label = { Text("Transacciones Recurrentes") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("recurring_transactions")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.FileDownload, contentDescription = "Exportar Datos") },
                    label = { Text("Exportar Datos") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        exportLauncher.launch("easyfinance_export_${System.currentTimeMillis()}.csv")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.FileUpload, contentDescription = "Importar Datos") },
                    label = { Text("Importar Datos") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        importLauncher.launch(mimeTypes)
                    }
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Numbers, contentDescription = "Reportes") },
                    label = { Text("Reportes") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("reports")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "Gráficos") },
                    label = { Text("Gráficos") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("charts")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Logout, contentDescription = "Cerrar Sesión") },
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text("Easy Finance") },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Abrir Menú")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nobglogo),
                    contentDescription = "Logo de la app",
                    modifier = Modifier
                        .size(350.dp)
                        .padding(bottom = 32.dp)
                )
                Text(
                    text = "Balance: ${decimalFormat.format(balance)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Total Ingresos: ${decimalFormat.format(totalIncomes)}",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Total Gastos: ${decimalFormat.format(totalExpenses)}",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = quote,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    maxLines = 3
                )
            }
        }
    }
}
