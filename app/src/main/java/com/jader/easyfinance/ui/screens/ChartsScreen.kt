package com.jader.easyfinance.ui.screens

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.jader.easyfinance.data.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
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
                    trySend(it.toObjects<Transaction>()).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }
    val transactions: State<List<Transaction>> = transactionsFlow.collectAsState(initial = emptyList())

    // Calculate income totals by category
    val incomeTotals: Map<String, Float> = transactions.value
        .filter { transaction: Transaction -> transaction.isIncome }
        .groupBy { transaction: Transaction -> transaction.category }
        .mapValues { entry: Map.Entry<String, List<Transaction>> ->
            entry.value.sumOf { transaction: Transaction -> transaction.amount.toDouble() }.toFloat()
        }

    // Calculate expense totals by category
    val expenseTotals: Map<String, Float> = transactions.value
        .filter { transaction: Transaction -> !transaction.isIncome }
        .groupBy { transaction: Transaction -> transaction.category }
        .mapValues { entry: Map.Entry<String, List<Transaction>> ->
            entry.value.sumOf { transaction: Transaction -> transaction.amount.toDouble() }.toFloat()
        }

    val (quincenaLabels, incomesQuincenales, expensesQuincenales) = remember(transactions) {
        calculateQuincenalTotals(transactions.value)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Gráficos") },
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
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Distribución de Ingresos",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            )
            PieChartView(dataMap = incomeTotals)

            Text(
                text = "Distribución de Gastos",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            )
            PieChartView(dataMap = expenseTotals)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Comparación Quincenal Gastos vs Ingresos",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            )
            BarComparisonChart(
                incomes = incomesQuincenales,
                expenses = expensesQuincenales,
                quincenaLabels = quincenaLabels,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun calculateQuincenalTotals(
    transactions: List<Transaction>
): Triple<List<String>, FloatArray, FloatArray> {
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    val today = Calendar.getInstance()

    val quincenaActualStart = Calendar.getInstance()
    val day = quincenaActualStart.get(Calendar.DAY_OF_MONTH)
    if (day <= 15) {
        quincenaActualStart.set(Calendar.DAY_OF_MONTH, 1)
    } else {
        quincenaActualStart.set(Calendar.DAY_OF_MONTH, 16)
    }
    quincenaActualStart.set(Calendar.HOUR_OF_DAY, 0)
    quincenaActualStart.set(Calendar.MINUTE, 0)
    quincenaActualStart.set(Calendar.SECOND, 0)
    quincenaActualStart.set(Calendar.MILLISECOND, 0)

    val quincenaPasadaStart = quincenaActualStart.clone() as Calendar
    quincenaPasadaStart.add(Calendar.DAY_OF_MONTH, -15)

    val quincenaSiguienteStart = quincenaActualStart.clone() as Calendar
    quincenaSiguienteStart.add(Calendar.DAY_OF_MONTH, 15)

    fun sumBetweenDates(
        start: Calendar,
        end: Calendar,
        isIncome: Boolean
    ): Float {
        return transactions.filter { transaction ->
            val tDateMillis = transaction.startDate ?: return@filter false
            val tDate = Date(tDateMillis)
            !tDate.before(start.time) && tDate.before(end.time) && transaction.isIncome == isIncome
        }.sumOf { transaction -> transaction.amount.toDouble() }.toFloat()
    }

    val quincenaPasadaEnd = quincenaPasadaStart.clone() as Calendar
    quincenaPasadaEnd.add(Calendar.DAY_OF_MONTH, 15)
    val quincenaActualEnd = quincenaActualStart.clone() as Calendar
    quincenaActualEnd.add(Calendar.DAY_OF_MONTH, 15)
    val quincenaSiguienteEnd = quincenaSiguienteStart.clone() as Calendar
    quincenaSiguienteEnd.add(Calendar.DAY_OF_MONTH, 15)

    val incomes = floatArrayOf(
        sumBetweenDates(quincenaPasadaStart, quincenaPasadaEnd, true),
        sumBetweenDates(quincenaActualStart, quincenaActualEnd, true),
        sumBetweenDates(quincenaSiguienteStart, quincenaSiguienteEnd, true)
    )
    val expenses = floatArrayOf(
        sumBetweenDates(quincenaPasadaStart, quincenaPasadaEnd, false),
        sumBetweenDates(quincenaActualStart, quincenaActualEnd, false),
        sumBetweenDates(quincenaSiguienteStart, quincenaSiguienteEnd, false)
    )

    val labels = listOf(
        dateFormat.format(quincenaPasadaStart.time),
        "${dateFormat.format(quincenaActualStart.time)} (Actual)",
        dateFormat.format(quincenaSiguienteStart.time)
    )

    return Triple(labels, incomes, expenses)
}

@Composable
fun PieChartView(dataMap: Map<String, Float>, modifier: Modifier = Modifier) {
    val entries = dataMap.map { PieEntry(it.value, it.key) }
    val colors = listOf(
        Color(0xFF4CAF50).toArgb(),
        Color(0xFFF44336).toArgb(),
        Color(0xFF2196F3).toArgb(),
        Color(0xFFFFC107).toArgb(),
        Color(0xFF9C27B0).toArgb(),
        Color(0xFF009688).toArgb()
    )

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        factory = { context ->
            com.github.mikephil.charting.charts.PieChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                description.isEnabled = false
                setUsePercentValues(true)
                setDrawEntryLabels(false)
                legend.orientation = Legend.LegendOrientation.VERTICAL
                legend.isWordWrapEnabled = true
                legend.textColor = android.graphics.Color.WHITE
                setExtraOffsets(5f, 10f, 5f, 5f)
                isRotationEnabled = true
                setHoleColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { pieChart ->
            val dataSet = PieDataSet(entries, "").apply {
                setColors(colors)
                sliceSpace = 2f
                valueTextSize = 12f
                valueTextColor = android.graphics.Color.WHITE
                valueFormatter = com.github.mikephil.charting.formatter.PercentFormatter(pieChart)
            }
            pieChart.data = PieData(dataSet)
            pieChart.invalidate()
        }
    )
}

@Composable
fun BarComparisonChart(
    incomes: FloatArray,
    expenses: FloatArray,
    quincenaLabels: List<String>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp),
        factory = { context ->
            CombinedChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                description.isEnabled = false
                legend.apply {
                    isEnabled = true
                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                    textColor = android.graphics.Color.WHITE
                    textSize = 14f
                }
                axisRight.isEnabled = false
                axisLeft.textColor = android.graphics.Color.WHITE
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                    textColor = android.graphics.Color.WHITE
                    valueFormatter = IndexAxisValueFormatter(quincenaLabels)
                    axisMinimum = -0.5f
                    axisMaximum = incomes.size - 0.5f
                }
                setDrawGridBackground(false)
                setPinchZoom(false)
                setScaleEnabled(false)
            }
        },
        update = { chart ->
            val barEntries = expenses.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
            val barDataSet = BarDataSet(barEntries, "Gastos").apply {
                color = android.graphics.Color.RED
                valueTextColor = android.graphics.Color.WHITE
                valueTextSize = 12f
                setDrawValues(true)
            }
            val barData = BarData(barDataSet).apply {
                barWidth = 0.3f
            }

            val lineEntries = incomes.mapIndexed { index, value -> Entry(index.toFloat(), value) }
            val lineDataSet = LineDataSet(lineEntries, "Ingresos").apply {
                color = android.graphics.Color.GREEN
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 5f
                circleHoleRadius = 2.5f
                setDrawValues(true)
                valueTextSize = 12f
                valueTextColor = android.graphics.Color.GREEN
            }
            val lineData = LineData(lineDataSet)

            chart.data = CombinedData().apply {
                setData(barData)
                setData(lineData)
            }
            chart.invalidate()
        }
    )
}