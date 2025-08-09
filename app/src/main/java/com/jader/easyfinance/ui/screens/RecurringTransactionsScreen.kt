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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.jader.easyfinance.data.RecurringTransactionTemplate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
fun RecurringTransactionsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableIntStateOf(0) } // 0 = Todos, 1 = Ingresos, 2 = Gastos
    var selectedDateFilter by remember { mutableIntStateOf(0) } // 0 = Todo, 1 = Últ. 7 días, 2 = Últ. mes
    var selectedCategory by remember { mutableStateOf("Todas") } // Filtro por categoría
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<RecurringTransactionTemplate?>(null) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = Firebase.firestore
    val templatesFlow: Flow<List<RecurringTransactionTemplate>> = callbackFlow {
        val listener = db.collection("users").document(userId)
            .collection("recurring_templates")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Error fetching templates: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.toObjects<RecurringTransactionTemplate>()).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }
    val templates by templatesFlow.collectAsState(initial = emptyList())
    val today = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val categories = listOf("Todas", "Sueldo", "Otros", "Alimentos", "Transporte", "Entretenimiento", "Hogar", "Impuestos")
    val filteredTemplates = remember(templates, selectedFilter, selectedDateFilter, selectedCategory) {
        var result = when (selectedFilter) {
            1 -> templates.filter { it.isIncome }
            2 -> templates.filter { !it.isIncome }
            else -> templates
        }
        result = when (selectedDateFilter) {
            1 -> { // Últimos 7 días
                val sevenDaysAgo = Calendar.getInstance(TimeZone.getDefault()).apply {
                    add(Calendar.DAY_OF_MONTH, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                result.filter { it.startDate != null && it.startDate >= sevenDaysAgo && it.startDate <= today.timeInMillis }
            }
            2 -> { // Último mes
                val oneMonthAgo = Calendar.getInstance(TimeZone.getDefault()).apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                result.filter { it.startDate != null && it.startDate >= oneMonthAgo && it.startDate <= today.timeInMillis }
            }
            else -> result // Todo
        }
        result = if (selectedCategory != "Todas") {
            result.filter { it.category == selectedCategory }
        } else {
            result
        }
        Log.d("TemplatesFilter", "Filtered Templates (Type=$selectedFilter, Date=$selectedDateFilter, Category=$selectedCategory): $result")
        result
    }
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
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selectedDateFilter == 0,
                    onClick = { selectedDateFilter = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Todo")
                }
                SegmentedButton(
                    selected = selectedDateFilter == 1,
                    onClick = { selectedDateFilter = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Últ. 7 días")
                }
                SegmentedButton(
                    selected = selectedDateFilter == 2,
                    onClick = { selectedDateFilter = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Últ. mes")
                }
            }
            ExposedDropdownMenuBox(
                expanded = isCategoryDropdownExpanded,
                onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = selectedCategory,
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
            if (filteredTemplates.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (selectedFilter) {
                            1 -> "No hay ingresos recurrentes"
                            2 -> "No hay gastos recurrentes"
                            else -> "No hay transacciones recurrentes"
                        } + when (selectedDateFilter) {
                            1 -> " en los últimos 7 días"
                            2 -> " en el último mes"
                            else -> ""
                        } + if (selectedCategory != "Todas") " para la categoría $selectedCategory" else "",
                        fontWeight = FontWeight.Medium,
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
                    items(filteredTemplates) { template ->
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
                                        text = "Categoría: ${template.category}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Monto: ${decimalFormat.format(template.amount)}"
                                    )
                                    Text(
                                        text = "Frecuencia: ${
                                            when (template.recurrenceType) {
                                                "WEEKLY" -> "Semanal"
                                                "BIWEEKLY" -> "Quincenal"
                                                "MONTHLY" -> "Mensual"
                                                else -> ""
                                            }
                                        }"
                                    )
                                    template.startDate?.let {
                                        Text(text = "Inicio: ${dateFormat.format(Date(it))}")
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        navController.navigate("add_recurring_transaction/${template.id}")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar"
                                        )
                                    }
                                    IconButton(onClick = {
                                        templateToDelete = template
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

    if (showDeleteDialog && templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que quieres eliminar esta transacción recurrente?") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        templateToDelete?.let { template ->
                            db.collection("users").document(userId).collection("recurring_templates")
                                .document(template.id).delete().await()
                            if (template.isIncome && template.category == "Sueldo" && template.startDate != null) {
                                val taxTemplate = db.collection("users").document(userId).collection("recurring_templates")
                                    .whereEqualTo("category", "Impuestos")
                                    .whereEqualTo("recurrenceType", template.recurrenceType)
                                    .whereEqualTo("startDate", template.startDate)
                                    .get().await()
                                taxTemplate.documents.forEach { taxDoc ->
                                    db.collection("users").document(userId).collection("recurring_templates")
                                        .document(taxDoc.id).delete().await()
                                }
                            }
                        }
                        showDeleteDialog = false
                        templateToDelete = null
                    }
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    templateToDelete = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}