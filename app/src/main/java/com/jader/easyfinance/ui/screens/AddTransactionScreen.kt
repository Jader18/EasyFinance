package com.jader.easyfinance.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.jader.easyfinance.data.Transaction
import com.jader.easyfinance.data.TransactionDao
import com.jader.easyfinance.ui.theme.EasyFinanceTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionDao: TransactionDao,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceType by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isRecurrenceDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val categories = if (isIncome) {
        listOf("Sueldo", "Otros")
    } else {
        listOf("Alimentos", "Transporte", "Entretenimiento", "Hogar", "Impuestos", "Otros")
    }
    val recurrenceTypes = listOf("Semanal", "Quincenal", "Mensual")
    val datePickerState = rememberDatePickerState()

    // Reiniciar categoría si no es válida
    if (category !in categories) {
        category = ""
    }
    // Reiniciar tipo de recurrencia si no es válida
    if (recurrenceType !in recurrenceTypes && isRecurring) {
        recurrenceType = ""
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Agregar Transacción") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = category,
                    onValueChange = { /* No permite edición manual */ },
                    label = { Text("Categoría") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tipo", fontSize = 16.sp)
                Switch(
                    checked = isIncome,
                    onCheckedChange = { isIncome = it },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(if (isIncome) "Ingreso" else "Gasto", fontSize = 14.sp)
            }
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recurrente", fontSize = 16.sp)
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            if (isRecurring) {
                ExposedDropdownMenuBox(
                    expanded = isRecurrenceDropdownExpanded,
                    onExpandedChange = { isRecurrenceDropdownExpanded = !isRecurrenceDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = recurrenceType,
                        onValueChange = { /* No permite edición manual */ },
                        label = { Text("Frecuencia") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRecurrenceDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isRecurrenceDropdownExpanded,
                        onDismissRequest = { isRecurrenceDropdownExpanded = false }
                    ) {
                        recurrenceTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    recurrenceType = type
                                    isRecurrenceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = startDate?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Seleccionar Fecha de Inicio"
                    )
                }
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            Button(onClick = {
                                startDate = datePickerState.selectedDateMillis
                                showDatePicker = false
                            }) {
                                Text("Aceptar")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDatePicker = false }) {
                                Text("Cancelar")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }
            Button(
                onClick = {
                    if (amount.isNotEmpty() && category.isNotEmpty()) {
                        if (isRecurring && (recurrenceType.isEmpty() || startDate == null)) {
                            Toast.makeText(
                                context,
                                "Por favor, completa los campos de recurrencia",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue != null) {
                                coroutineScope.launch {
                                    // Insertar transacción principal
                                    val recurrenceTypeValue = if (isRecurring) {
                                        when (recurrenceType) {
                                            "Semanal" -> "WEEKLY"
                                            "Quincenal" -> "BIWEEKLY"
                                            "Mensual" -> "MONTHLY"
                                            else -> null
                                        }
                                    } else null
                                    val transaction = Transaction(
                                        amount = amountValue,
                                        category = category,
                                        isIncome = isIncome,
                                        isRecurring = isRecurring,
                                        recurrenceType = recurrenceTypeValue,
                                        startDate = startDate
                                    )
                                    transactionDao.insert(transaction)

                                    // Calcular e insertar impuestos si es Sueldo
                                    if (isIncome && category == "Sueldo") {
                                        // Calcular INSS (7%)
                                        val inss = amountValue * 0.07

                                        // Calcular IR
                                        val periodsPerYear = when (recurrenceTypeValue) {
                                            "WEEKLY" -> 52.0
                                            "BIWEEKLY" -> 26.0
                                            "MONTHLY" -> 12.0
                                            else -> 1.0 // No recurrente
                                        }
                                        val annualGrossSalary = amountValue * periodsPerYear
                                        val annualNetSalary = annualGrossSalary - (inss * periodsPerYear)
                                        val annualIR = when {
                                            annualNetSalary <= 100_000 -> 0.0
                                            annualNetSalary <= 200_000 -> (annualNetSalary - 100_000) * 0.15
                                            annualNetSalary <= 350_000 -> (annualNetSalary - 200_000) * 0.20 + 15_000
                                            annualNetSalary <= 500_000 -> (annualNetSalary - 350_000) * 0.25 + 45_000
                                            else -> (annualNetSalary - 500_000) * 0.30 + 82_500
                                        }
                                        val irPerPeriod = annualIR / periodsPerYear
                                        val totalTax = inss + irPerPeriod

                                        // Insertar transacción de impuestos
                                        transactionDao.insert(
                                            Transaction(
                                                amount = totalTax,
                                                category = "Impuestos",
                                                isIncome = false,
                                                isRecurring = isRecurring,
                                                recurrenceType = recurrenceTypeValue,
                                                startDate = startDate
                                            )
                                        )
                                    }

                                    Toast.makeText(
                                        context,
                                        "Transacción guardada",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigateUp()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Por favor, ingresa un monto válido",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Por favor, completa todos los campos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddTransactionScreenPreview() {
    EasyFinanceTheme {
        AddTransactionScreen(
            navController = rememberNavController(),
            transactionDao = object : TransactionDao {
                override suspend fun insert(transaction: Transaction) {}
                override fun getAllTransactions(): Flow<List<Transaction>> = emptyFlow()
                override fun getRecurringTransactions(): Flow<List<Transaction>> = emptyFlow()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}