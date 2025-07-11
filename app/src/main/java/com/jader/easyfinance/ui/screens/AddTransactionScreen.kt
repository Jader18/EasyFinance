package com.jader.easyfinance.ui.screens

import android.util.Log
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.jader.easyfinance.data.RecurringTransactionTemplate
import com.jader.easyfinance.data.Transaction
import com.jader.easyfinance.data.TransactionDao
import com.jader.easyfinance.ui.theme.EasyFinanceTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionDao: TransactionDao,
    transactionId: Int? = null,
    isTemplate: Boolean = false,
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
    var existingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var existingTemplate by remember { mutableStateOf<RecurringTransactionTemplate?>(null) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    // Cargar datos existentes si se pasa un transactionId
    if (transactionId != null) {
        LaunchedEffect(transactionId, isTemplate) {
            if (isTemplate) {
                val templates = transactionDao.getRecurringTemplates().first()
                existingTemplate = templates.find { it.id == transactionId }
                existingTemplate?.let { template ->
                    amount = DecimalFormat("#,##0.00").format(template.amount)
                    category = template.category
                    isIncome = template.isIncome
                    isRecurring = template.isRecurring
                    recurrenceType = when (template.recurrenceType) {
                        "WEEKLY" -> "Semanal"
                        "BIWEEKLY" -> "Quincenal"
                        "MONTHLY" -> "Mensual"
                        else -> ""
                    }
                    startDate = template.startDate
                    startDate?.let {
                        Log.d("DateDebug", "Loaded template date: ${dateFormat.format(Date(it))}, TimeZone: ${TimeZone.getDefault().id}")
                    }
                }
            } else {
                val transactions = transactionDao.getAllTransactions().first()
                existingTransaction = transactions.find { it.id == transactionId }
                existingTransaction?.let { transaction ->
                    amount = DecimalFormat("#,##0.00").format(transaction.amount)
                    category = transaction.category
                    isIncome = transaction.isIncome
                    isRecurring = transaction.isRecurring
                    recurrenceType = when (transaction.recurrenceType) {
                        "WEEKLY" -> "Semanal"
                        "BIWEEKLY" -> "Quincenal"
                        "MONTHLY" -> "Mensual"
                        else -> ""
                    }
                    startDate = transaction.startDate
                    startDate?.let {
                        Log.d("DateDebug", "Loaded transaction date: ${dateFormat.format(Date(it))}, TimeZone: ${TimeZone.getDefault().id}")
                    }
                }
            }
        }
    }

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
                title = { Text(if (isTemplate) "Editar Plantilla Recurrente" else if (transactionId == null) "Agregar Transacción" else "Editar Transacción") },
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
                            val formattedDate = dateFormat.format(Date(it))
                            Log.d("DateDebug", "Displayed startDate: $formattedDate, Raw: $it, TimeZone: ${TimeZone.getDefault().id}")
                            formattedDate
                        } ?: "Seleccionar Fecha de Inicio"
                    )
                }
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            Button(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    // Ajustar la fecha para que sea al inicio del día en la zona horaria local
                                    val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
                                        timeInMillis = millis
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    startDate = calendar.timeInMillis
                                    Log.d("DateDebug", "Selected date: ${dateFormat.format(Date(startDate!!))}, Raw: $startDate, TimeZone: ${TimeZone.getDefault().id}")
                                }
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
                                    // Preparar transacción o plantilla
                                    val recurrenceTypeValue = if (isRecurring) {
                                        when (recurrenceType) {
                                            "Semanal" -> "WEEKLY"
                                            "Quincenal" -> "BIWEEKLY"
                                            "Mensual" -> "MONTHLY"
                                            else -> null
                                        }
                                    } else null
                                    if (isTemplate) {
                                        val template = RecurringTransactionTemplate(
                                            id = transactionId ?: 0,
                                            amount = amountValue,
                                            category = category,
                                            isIncome = isIncome,
                                            isRecurring = isRecurring,
                                            recurrenceType = recurrenceTypeValue,
                                            startDate = startDate
                                        )
                                        if (transactionId != null) {
                                            transactionDao.updateTemplate(template)
                                        } else {
                                            transactionDao.insertTemplate(template)
                                        }
                                        // Si es Sueldo, gestionar plantilla de impuestos
                                        if (isIncome && category == "Sueldo") {
                                            val inss = amountValue * 0.07
                                            val periodsPerYear = when (recurrenceTypeValue) {
                                                "WEEKLY" -> 52.0
                                                "BIWEEKLY" -> 24.0
                                                "MONTHLY" -> 12.0
                                                else -> 1.0
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
                                            Log.d("TaxCalculation", "Sueldo: $amountValue, INSS: $inss, IR: $irPerPeriod, Total Tax: $totalTax, Recurrence: $recurrenceTypeValue, Date: ${startDate?.let { dateFormat.format(Date(it)) }}")
                                            val taxTemplate = transactionDao.getTaxTemplate(startDate!!, recurrenceTypeValue)
                                            if (taxTemplate != null) {
                                                transactionDao.updateTemplate(
                                                    taxTemplate.copy(
                                                        amount = totalTax,
                                                        isRecurring = isRecurring,
                                                        recurrenceType = recurrenceTypeValue,
                                                        startDate = startDate
                                                    )
                                                )
                                            } else if (isRecurring) {
                                                transactionDao.insertTemplate(
                                                    RecurringTransactionTemplate(
                                                        amount = totalTax,
                                                        category = "Impuestos",
                                                        isIncome = false,
                                                        isRecurring = isRecurring,
                                                        recurrenceType = recurrenceTypeValue,
                                                        startDate = startDate
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        val transaction = Transaction(
                                            id = transactionId ?: 0,
                                            amount = amountValue,
                                            category = category,
                                            isIncome = isIncome,
                                            isRecurring = isRecurring,
                                            recurrenceType = recurrenceTypeValue,
                                            startDate = startDate
                                        )
                                        // Si es recurrente, gestionar la plantilla
                                        if (isRecurring) {
                                            val template = RecurringTransactionTemplate(
                                                id = transactionId ?: 0,
                                                amount = amountValue,
                                                category = category,
                                                isIncome = isIncome,
                                                isRecurring = isRecurring,
                                                recurrenceType = recurrenceTypeValue,
                                                startDate = startDate
                                            )
                                            if (transactionId != null) {
                                                transactionDao.updateTemplate(template)
                                            } else {
                                                transactionDao.insertTemplate(template)
                                            }
                                        }
                                        // Si es edición, actualizar; si no, insertar
                                        if (transactionId != null) {
                                            transactionDao.update(transaction)
                                        } else {
                                            transactionDao.insert(transaction)
                                        }
                                        // Si es Sueldo, gestionar transacción y plantilla de impuestos
                                        if (isIncome && category == "Sueldo") {
                                            val inss = amountValue * 0.07
                                            val periodsPerYear = when (recurrenceTypeValue) {
                                                "WEEKLY" -> 52.0
                                                "BIWEEKLY" -> 24.0
                                                "MONTHLY" -> 12.0
                                                else -> 1.0
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
                                            Log.d("TaxCalculation", "Sueldo: $amountValue, INSS: $inss, IR: $irPerPeriod, Total Tax: $totalTax, Recurrence: $recurrenceTypeValue, Date: ${startDate?.let { dateFormat.format(Date(it)) }}")
                                            if (transactionId != null) {
                                                val transactions = transactionDao.getAllTransactions().first()
                                                val taxTransaction = transactions.find {
                                                    !it.isIncome && it.category == "Impuestos" && it.startDate == startDate
                                                            && it.recurrenceType == recurrenceTypeValue
                                                }
                                                taxTransaction?.let {
                                                    transactionDao.update(
                                                        it.copy(
                                                            amount = totalTax,
                                                            isRecurring = isRecurring,
                                                            recurrenceType = recurrenceTypeValue,
                                                            startDate = startDate
                                                        )
                                                    )
                                                } ?: run {
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
                                            } else {
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
                                            if (isRecurring) {
                                                val taxTemplate = transactionDao.getTaxTemplate(startDate!!, recurrenceTypeValue)
                                                if (taxTemplate != null) {
                                                    transactionDao.updateTemplate(
                                                        taxTemplate.copy(
                                                            amount = totalTax,
                                                            isRecurring = isRecurring,
                                                            recurrenceType = recurrenceTypeValue,
                                                            startDate = startDate
                                                        )
                                                    )
                                                } else {
                                                    transactionDao.insertTemplate(
                                                        RecurringTransactionTemplate(
                                                            amount = totalTax,
                                                            category = "Impuestos",
                                                            isIncome = false,
                                                            isRecurring = isRecurring,
                                                            recurrenceType = recurrenceTypeValue,
                                                            startDate = startDate
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        if (isTemplate) {
                                            if (transactionId == null) "Plantilla guardada" else "Plantilla actualizada"
                                        } else {
                                            if (transactionId == null) "Transacción guardada" else "Transacción actualizada"
                                        },
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
                Text(if (isTemplate) "Guardar Plantilla" else if (transactionId == null) "Guardar" else "Actualizar")
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