package com.jader.easyfinance.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.jader.easyfinance.data.RecurringTransactionTemplate
import com.jader.easyfinance.data.Transaction
import com.jader.easyfinance.data.TransactionDao
import com.jader.easyfinance.ui.theme.EasyFinanceTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    navController: NavController,
    transactionDao: TransactionDao,
    modifier: Modifier = Modifier
) {
    var showStopDialog by remember { mutableStateOf(false) }
    var templateToStop by remember { mutableStateOf<RecurringTransactionTemplate?>(null) }
    val recurringTemplates by transactionDao.getRecurringTemplates().collectAsState(initial = emptyList())
    val decimalFormat = DecimalFormat("C$ #,##0.00")
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Transacciones Recurrentes") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
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
            if (recurringTemplates.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No hay transacciones recurrentes",
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
                    items(recurringTemplates) { template ->
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
                                        text = "${if (template.isIncome) "Ingreso" else "Gasto"}: ${decimalFormat.format(template.amount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Categoría: ${template.category}",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Recurrente: ${
                                            when (template.recurrenceType) {
                                                "WEEKLY" -> "Semanal"
                                                "BIWEEKLY" -> "Quincenal"
                                                "MONTHLY" -> "Mensual"
                                                else -> ""
                                            }
                                        }",
                                        fontSize = 14.sp
                                    )
                                    template.startDate?.let {
                                        Text(
                                            text = "Inicio: ${dateFormat.format(Date(it))}",
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        navController.navigate("add_transaction_template/${template.id}")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar"
                                        )
                                    }
                                    IconButton(onClick = {
                                        templateToStop = template
                                        showStopDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Detener recurrencia"
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

    // Diálogo de confirmación para detener recurrencia
    if (showStopDialog && templateToStop != null) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Confirmar detener recurrencia") },
            text = { Text("¿Deseas detener la recurrencia permanentemente? Esto no eliminará transacciones existentes.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            templateToStop?.let { template ->
                                transactionDao.updateTemplate(template.copy(isRecurring = false))
                                // Si es Sueldo, detener la recurrencia del impuesto asociado
                                if (template.isIncome && template.category == "Sueldo") {
                                    template.startDate?.let { startDate ->
                                        template.recurrenceType?.let { recurrenceType ->
                                            val taxTemplate = transactionDao.getTaxTemplate(startDate, recurrenceType)
                                            taxTemplate?.let {
                                                transactionDao.updateTemplate(it.copy(isRecurring = false))
                                            }
                                        }
                                    }
                                }
                            }
                            showStopDialog = false
                            templateToStop = null
                        }
                    }
                ) {
                    Text("Detener")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showStopDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RecurringTransactionsScreenPreview() {
    EasyFinanceTheme {
        RecurringTransactionsScreen(
            navController = rememberNavController(),
            transactionDao = object : TransactionDao {
                override suspend fun insert(transaction: Transaction) {}
                override suspend fun insertTemplate(template: RecurringTransactionTemplate) {}
                override suspend fun update(transaction: Transaction) {}
                override suspend fun updateTemplate(template: RecurringTransactionTemplate) {}
                override suspend fun delete(transaction: Transaction) {}
                override suspend fun deleteTemplate(template: RecurringTransactionTemplate) {}
                override fun getAllTransactions(): Flow<List<Transaction>> = emptyFlow()
                override fun getRecurringTemplates(): Flow<List<RecurringTransactionTemplate>> = emptyFlow()
                override fun getRecurringTransactions(): Flow<List<Transaction>> = emptyFlow()
                override suspend fun getTaxTransaction(startDate: Long, recurrenceType: String?): Transaction? = null
                override suspend fun getTaxTemplate(startDate: Long, recurrenceType: String?): RecurringTransactionTemplate? = null
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}