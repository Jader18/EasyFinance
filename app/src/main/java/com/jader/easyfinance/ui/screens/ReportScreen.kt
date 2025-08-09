
package com.jader.easyfinance.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jader.easyfinance.data.DataManager
import com.jader.easyfinance.data.ReportFilterParams
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var filterParams by remember { mutableStateOf(ReportFilterParams()) }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var selectedDateRange by remember { mutableIntStateOf(0) } // 0 = Todo, 1 = Últ. 7 días, 2 = Últ. 30 días, 3 = Este mes, 4 = Personalizado
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    val categories = listOf("Todas", "Sueldo", "Otros", "Alimentos", "Transporte", "Entretenimiento", "Hogar", "Impuestos")
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? ->
            uri?.let {
                isExporting = true
                coroutineScope.launch {
                    DataManager.exportFilteredReport(context, it, filterParams.copy(category = if (selectedCategory != "Todas") selectedCategory else null))
                    isExporting = false
                }
            }
        }
    )

    // Estado para los DatePickers
    val startDatePickerState = remember {
        DatePickerState(
            initialSelectedDateMillis = filterParams.startDate ?: System.currentTimeMillis(),
            initialDisplayedMonthMillis = filterParams.startDate ?: System.currentTimeMillis(),
            yearRange = IntRange(2020, 2030),
            initialDisplayMode = DisplayMode.Picker,
            locale = Locale.getDefault()
        )
    }
    val endDatePickerState = remember {
        DatePickerState(
            initialSelectedDateMillis = filterParams.endDate ?: System.currentTimeMillis(),
            initialDisplayedMonthMillis = filterParams.endDate ?: System.currentTimeMillis(),
            yearRange = IntRange(2020, 2030),
            initialDisplayMode = DisplayMode.Picker,
            locale = Locale.getDefault()
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Generar Reporte") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de fechas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Rango de fechas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = selectedDateRange == 0,
                            onClick = {
                                selectedDateRange = 0
                                filterParams = filterParams.copy(startDate = null, endDate = null)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                        ) {
                            Text("Todo")
                        }
                        SegmentedButton(
                            selected = selectedDateRange == 1,
                            onClick = {
                                selectedDateRange = 1
                                val now = Calendar.getInstance(TimeZone.getDefault()).apply {
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }.timeInMillis
                                val sevenDaysAgo = Calendar.getInstance(TimeZone.getDefault()).apply {
                                    add(Calendar.DAY_OF_MONTH, -7)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                filterParams = filterParams.copy(startDate = sevenDaysAgo, endDate = now)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                        ) {
                            Text("Últ. 7 días")
                        }
                        SegmentedButton(
                            selected = selectedDateRange == 2,
                            onClick = {
                                selectedDateRange = 2
                                val now = Calendar.getInstance(TimeZone.getDefault()).apply {
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }.timeInMillis
                                val thirtyDaysAgo = Calendar.getInstance(TimeZone.getDefault()).apply {
                                    add(Calendar.DAY_OF_MONTH, -30)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                filterParams = filterParams.copy(startDate = thirtyDaysAgo, endDate = now)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                        ) {
                            Text("Últ. 30 días")
                        }
                        SegmentedButton(
                            selected = selectedDateRange == 3,
                            onClick = {
                                selectedDateRange = 3
                                val now = Calendar.getInstance(TimeZone.getDefault())
                                val startOfMonth = Calendar.getInstance(TimeZone.getDefault()).apply {
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                now.set(Calendar.HOUR_OF_DAY, 23)
                                now.set(Calendar.MINUTE, 59)
                                now.set(Calendar.SECOND, 59)
                                now.set(Calendar.MILLISECOND, 999)
                                filterParams = filterParams.copy(startDate = startOfMonth, endDate = now.timeInMillis)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                        ) {
                            Text("Este mes")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inicio: ${filterParams.startDate?.let { dateFormatter.format(Date(it)) } ?: "No seleccionada"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Seleccionar")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fin: ${filterParams.endDate?.let { dateFormatter.format(Date(it)) } ?: "No seleccionada"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Seleccionar")
                        }
                    }
                }
            }

            // DatePickerDialog para fecha de inicio
            if (showStartDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        Button(onClick = {
                            startDatePickerState.selectedDateMillis?.let { millis ->
                                val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
                                    timeInMillis = millis
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                filterParams = filterParams.copy(startDate = calendar.timeInMillis)
                                selectedDateRange = 4 // Personalizado
                            }
                            showStartDatePicker = false
                        }) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showStartDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = startDatePickerState)
                }
            }

            // DatePickerDialog para fecha de fin
            if (showEndDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        Button(onClick = {
                            endDatePickerState.selectedDateMillis?.let { millis ->
                                val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
                                    timeInMillis = millis
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }
                                filterParams = filterParams.copy(endDate = calendar.timeInMillis)
                                selectedDateRange = 4 // Personalizado
                            }
                            showEndDatePicker = false
                        }) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showEndDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = endDatePickerState)
                }
            }

            // Sección de tipos de transacción
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tipos de transacción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterParams.showIncomes,
                            onClick = {
                                if (!filterParams.showIncomes || filterParams.showExpenses) {
                                    filterParams = filterParams.copy(showIncomes = !filterParams.showIncomes)
                                } else {
                                    Toast.makeText(context, "Debe seleccionar al menos un tipo", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text("Ingresos") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = filterParams.showExpenses,
                            onClick = {
                                if (!filterParams.showExpenses || filterParams.showIncomes) {
                                    filterParams = filterParams.copy(showExpenses = !filterParams.showExpenses)
                                } else {
                                    Toast.makeText(context, "Debe seleccionar al menos un tipo", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text("Gastos") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterParams.showRecurring,
                            onClick = {
                                if (!filterParams.showRecurring || filterParams.showNonRecurring) {
                                    filterParams = filterParams.copy(showRecurring = !filterParams.showRecurring)
                                } else {
                                    Toast.makeText(context, "Debe seleccionar al menos un tipo", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text("Recurrentes") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = filterParams.showNonRecurring,
                            onClick = {
                                if (!filterParams.showNonRecurring || filterParams.showRecurring) {
                                    filterParams = filterParams.copy(showNonRecurring = !filterParams.showNonRecurring)
                                } else {
                                    Toast.makeText(context, "Debe seleccionar al menos un tipo", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text("No recurrentes") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sección de categoría
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Categoría",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = selectedCategory,
                            onValueChange = { /* No permite edición manual */ },
                            label = { Text("Seleccionar categoría") },
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
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Resumen de filtros
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Resumen de filtros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append("Tipos: ")
                            if (filterParams.showIncomes && filterParams.showExpenses) append("Todos")
                            else if (filterParams.showIncomes) append("Ingresos")
                            else if (filterParams.showExpenses) append("Gastos")
                            else append("Ninguno")
                            append("\nRecurrencia: ")
                            if (filterParams.showRecurring && filterParams.showNonRecurring) append("Todos")
                            else if (filterParams.showRecurring) append("Recurrentes")
                            else if (filterParams.showNonRecurring) append("No recurrentes")
                            else append("Ninguno")
                            append("\nRango: ")
                            if (filterParams.startDate == null && filterParams.endDate == null) append("Todo")
                            else append("${filterParams.startDate?.let { dateFormatter.format(Date(it)) } ?: "Inicio"} - ${filterParams.endDate?.let { dateFormatter.format(Date(it)) } ?: "Fin"}")
                            append("\nCategoría: $selectedCategory")
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Botón de exportar
            Button(
                onClick = {
                    if (!filterParams.showIncomes && !filterParams.showExpenses) {
                        Toast.makeText(context, "Debe seleccionar al menos un tipo de transacción", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!filterParams.showRecurring && !filterParams.showNonRecurring) {
                        Toast.makeText(context, "Debe seleccionar al menos un tipo de recurrencia", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    exportLauncher.launch("reporte_${System.currentTimeMillis()}.csv")
                },
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Exportando...")
                } else {
                    Text("Exportar Reporte")
                }
            }
        }
    }
}
