package com.jader.easyfinance.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jader.easyfinance.data.Transaction
import com.jader.easyfinance.data.TransactionDao
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavController,
    transactionDao: TransactionDao,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableIntStateOf(0) } // 0 = Todos, 1 = Ingresos, 2 = Gastos
    var showDeleteDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    val transactions by transactionDao.getAllTransactions().collectAsState(initial = emptyList())
    val filteredTransactions = when (selectedFilter) {
        1 -> transactions.filter { it.isIncome }
        2 -> transactions.filter { !it.isIncome }
        else -> transactions
    }
    val decimalFormat = DecimalFormat("C$ #,##0.00")
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    val totalIncomes = filteredTransactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpenses = filteredTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Transacciones") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Todos")
                }
                SegmentedButton(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Ingresos")
                }
                SegmentedButton(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Gastos")
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Total Ingresos: ${decimalFormat.format(totalIncomes)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Gastos: ${decimalFormat.format(totalExpenses)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (filteredTransactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (selectedFilter) {
                            1 -> "No hay ingresos"
                            2 -> "No hay gastos"
                            else -> "No hay transacciones"
                        },
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${if (transaction.isIncome) "Ingreso" else "Gasto"}: ${decimalFormat.format(transaction.amount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Categoría: ${transaction.category}",
                                        fontSize = 14.sp
                                    )
                                    if (transaction.isRecurring) {
                                        Text(
                                            text = "Recurrente: ${
                                                when (transaction.recurrenceType) {
                                                    "WEEKLY" -> "Semanal"
                                                    "BIWEEKLY" -> "Quincenal"
                                                    "MONTHLY" -> "Mensual"
                                                    else -> ""
                                                }
                                            }",
                                            fontSize = 14.sp
                                        )
                                        transaction.startDate?.let {
                                            Text(
                                                text = "Inicio: ${dateFormat.format(Date(it))}",
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        navController.navigate("add_transaction/${transaction.id}")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar"
                                        )
                                    }
                                    IconButton(onClick = {
                                        transactionToDelete = transaction
                                        showDeleteDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminación
    if (showDeleteDialog && transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("No se puede recuperar la transacción específica.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            transactionToDelete?.let { transaction ->
                                transactionDao.delete(transaction)
                                if (transaction.isIncome && transaction.category == "Sueldo") {
                                    transaction.startDate?.let { startDate ->
                                        transaction.recurrenceType?.let { recurrenceType ->
                                            val taxTransaction = transactionDao.getTaxTransaction(startDate, recurrenceType)
                                            Log.d("TaxDeleteDebug", "Tax transaction found: $taxTransaction")
                                            taxTransaction?.let { transactionDao.delete(it) }
                                        }
                                    }
                                }
                            }
                            showDeleteDialog = false
                            transactionToDelete = null
                        }
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}