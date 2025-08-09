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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jader.easyfinance.data.RecurringTransactionTemplate
import com.jader.easyfinance.data.Transaction
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    transactionId: String? = null,
    isTemplate: Boolean = false,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(isTemplate) }
    var recurrenceType by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var isTypeDropdownExpanded by remember { mutableStateOf(false) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isRecurrenceDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val typeOptions = listOf("Ingreso", "Gasto")
    val categories = if (isIncome) {
        listOf("Sueldo", "Otros")
    } else {
        listOf("Alimentos", "Transporte", "Entretenimiento", "Hogar", "Impuestos", "Otros")
    }
    val recurrenceTypes = listOf("Semanal", "Quincenal", "Mensual")
    val datePickerState = remember {
        androidx.compose.material3.DatePickerState(
            initialSelectedDateMillis = startDate ?: System.currentTimeMillis(),
            initialDisplayedMonthMillis = startDate ?: System.currentTimeMillis(),
            yearRange = IntRange(2020, 2030),
            initialDisplayMode = androidx.compose.material3.DisplayMode.Picker,
            locale = Locale.getDefault()
        )
    }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = Firebase.firestore

    // Load existing data if transactionId is provided
    LaunchedEffect(transactionId, isTemplate) {
        if (transactionId != null) {
            try {
                if (isTemplate) {
                    val template = db.collection("users").document(userId).collection("recurring_templates")
                        .document(transactionId).get().await().toObject(RecurringTransactionTemplate::class.java)
                    template?.let {
                        amount = DecimalFormat("#,##0.00").format(it.amount)
                        category = it.category
                        isIncome = it.isIncome
                        isRecurring = it.isRecurring
                        recurrenceType = when (it.recurrenceType) {
                            "WEEKLY" -> "Semanal"
                            "BIWEEKLY" -> "Quincenal"
                            "MONTHLY" -> "Mensual"
                            else -> ""
                        }
                        startDate = it.startDate
                        startDate?.let { date ->
                            Log.d("DateDebug", "Loaded template date: ${dateFormat.format(Date(date))}")
                        }
                    }
                } else {
                    val transaction = db.collection("users").document(userId).collection("transactions")
                        .document(transactionId).get().await().toObject(Transaction::class.java)
                    transaction?.let {
                        amount = DecimalFormat("#,##0.00").format(it.amount)
                        category = it.category
                        isIncome = it.isIncome
                        isRecurring = it.isRecurring
                        recurrenceType = when (it.recurrenceType) {
                            "WEEKLY" -> "Semanal"
                            "BIWEEKLY" -> "Quincenal"
                            "MONTHLY" -> "Mensual"
                            else -> ""
                        }
                        startDate = it.startDate
                        startDate?.let { date ->
                            Log.d("DateDebug", "Loaded transaction date: ${dateFormat.format(Date(date))}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FirestoreError", "Error loading transaction/template: ${e.message}")
                Toast.makeText(context, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Reset category and recurrence type if invalid
    if (category !in categories) category = ""
    if (isRecurring && recurrenceType !in recurrenceTypes) recurrenceType = ""

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (isTemplate) "Editar Plantilla Recurrente" else if (transactionId == null) "Agregar Transacción" else "Editar Transacción") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Type dropdown (Ingreso/Gasto)
            ExposedDropdownMenuBox(
                expanded = isTypeDropdownExpanded,
                onExpandedChange = { isTypeDropdownExpanded = !isTypeDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = if (isIncome) "Ingreso" else "Gasto",
                    onValueChange = { /* No permite edición manual */ },
                    label = { Text("Tipo") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isTypeDropdownExpanded,
                    onDismissRequest = { isTypeDropdownExpanded = false }
                ) {
                    typeOptions.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                isIncome = type == "Ingreso"
                                category = "" // Reset category when type changes
                                isTypeDropdownExpanded = false
                                Log.d("AddTransaction", "Selected type: $type, isIncome: $isIncome")
                            }
                        )
                    }
                }
            }
            // Amount field
            TextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = isCategoryDropdownExpanded,
                onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = category,
                    onValueChange = { /* No permite edición manual */ },
                    label = { Text("Categoría") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            // Recurring toggle
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
            // Date picker
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = startDate?.let {
                        dateFormat.format(Date(it))
                    } ?: if (isRecurring) "Seleccionar Fecha de Inicio" else "Seleccionar Fecha de Transacción"
                )
            }
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        Button(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
                                    timeInMillis = millis
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                    add(Calendar.DAY_OF_MONTH, 1) // Add one day to the selected date
                                }
                                startDate = calendar.timeInMillis
                                Log.d("DateDebug", "Selected date (adjusted +1 day): ${dateFormat.format(Date(startDate!!))}")
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
            // Recurrence type dropdown (if recurring)
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
            }
            // Save button
            Button(
                onClick = {
                    if (amount.isNotEmpty() && category.isNotEmpty()) {
                        if (isRecurring && (recurrenceType.isEmpty() || startDate == null)) {
                            Toast.makeText(context, "Por favor, completa los campos de recurrencia", Toast.LENGTH_SHORT).show()
                        } else {
                            val amountValue = amount.replace(",", "").toDoubleOrNull()
                            if (amountValue != null) {
                                coroutineScope.launch {
                                    try {
                                        val recurrenceTypeValue = if (isRecurring) {
                                            when (recurrenceType) {
                                                "Semanal" -> "WEEKLY"
                                                "Quincenal" -> "BIWEEKLY"
                                                "Mensual" -> "MONTHLY"
                                                else -> null
                                            }
                                        } else null
                                        Log.d("AddTransaction", "Saving with isIncome: $isIncome, category: $category, amount: $amountValue")
                                        if (isTemplate) {
                                            val template = RecurringTransactionTemplate(
                                                id = transactionId ?: db.collection("users").document(userId).collection("recurring_templates").document().id,
                                                amount = amountValue,
                                                category = category,
                                                isIncome = isIncome,
                                                isRecurring = isRecurring,
                                                recurrenceType = recurrenceTypeValue,
                                                startDate = startDate
                                            )
                                            Log.d("FirestoreSave", "Saving template: $template")
                                            db.collection("users").document(userId).collection("recurring_templates")
                                                .document(template.id).set(template).await()
                                            // Tax calculation for Sueldo
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
                                                Log.d("TaxCalculation", "Sueldo: $amountValue, INSS: $inss, IR: $irPerPeriod, Total Tax: $totalTax")
                                                val taxTemplateId = db.collection("users").document(userId).collection("recurring_templates").document().id
                                                val taxTemplate = RecurringTransactionTemplate(
                                                    id = taxTemplateId,
                                                    amount = totalTax,
                                                    category = "Impuestos",
                                                    isIncome = false,
                                                    isRecurring = isRecurring,
                                                    recurrenceType = recurrenceTypeValue,
                                                    startDate = startDate
                                                )
                                                Log.d("FirestoreSave", "Saving tax template: $taxTemplate")
                                                db.collection("users").document(userId).collection("recurring_templates").document(taxTemplateId)
                                                    .set(taxTemplate).await()
                                            }
                                        } else {
                                            val transaction = Transaction(
                                                id = transactionId ?: db.collection("users").document(userId).collection("transactions").document().id,
                                                amount = amountValue,
                                                category = category,
                                                isIncome = isIncome,
                                                isRecurring = isRecurring,
                                                recurrenceType = recurrenceTypeValue,
                                                startDate = startDate
                                            )
                                            Log.d("FirestoreSave", "Saving transaction: $transaction")
                                            db.collection("users").document(userId).collection("transactions")
                                                .document(transaction.id).set(transaction).await()
                                            if (isRecurring) {
                                                val templateId = transactionId ?: db.collection("users").document(userId).collection("recurring_templates").document().id
                                                val template = RecurringTransactionTemplate(
                                                    id = templateId,
                                                    amount = amountValue,
                                                    category = category,
                                                    isIncome = isIncome,
                                                    isRecurring = isRecurring,
                                                    recurrenceType = recurrenceTypeValue,
                                                    startDate = startDate
                                                )
                                                Log.d("FirestoreSave", "Saving template: $template")
                                                db.collection("users").document(userId).collection("recurring_templates")
                                                    .document(templateId).set(template).await()
                                            }
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
                                                Log.d("TaxCalculation", "Sueldo: $amountValue, INSS: $inss, IR: $irPerPeriod, Total Tax: $totalTax")
                                                val taxTransactionId = db.collection("users").document(userId).collection("transactions").document().id
                                                val taxTransaction = Transaction(
                                                    id = taxTransactionId,
                                                    amount = totalTax,
                                                    category = "Impuestos",
                                                    isIncome = false,
                                                    isRecurring = isRecurring,
                                                    recurrenceType = recurrenceTypeValue,
                                                    startDate = startDate
                                                )
                                                Log.d("FirestoreSave", "Saving tax transaction: $taxTransaction")
                                                db.collection("users").document(userId).collection("transactions").document(taxTransactionId)
                                                    .set(taxTransaction).await()
                                                if (isRecurring) {
                                                    val taxTemplateId = db.collection("users").document(userId).collection("recurring_templates").document().id
                                                    val taxTemplate = RecurringTransactionTemplate(
                                                        id = taxTemplateId,
                                                        amount = totalTax,
                                                        category = "Impuestos",
                                                        isIncome = false,
                                                        isRecurring = isRecurring,
                                                        recurrenceType = recurrenceTypeValue,
                                                        startDate = startDate
                                                    )
                                                    Log.d("FirestoreSave", "Saving tax template: $taxTemplate")
                                                    db.collection("users").document(userId).collection("recurring_templates").document(taxTemplateId)
                                                        .set(taxTemplate).await()
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
                                    } catch (e: Exception) {
                                        Log.e("FirestoreError", "Error saving transaction/template: ${e.message}")
                                        Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Por favor, ingresa un monto válido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isTemplate) "Guardar Plantilla" else if (transactionId == null) "Guardar" else "Actualizar")
            }
        }
    }
}