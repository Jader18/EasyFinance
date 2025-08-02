package com.jader.easyfinance.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.jader.easyfinance.data.Transaction
import com.jader.easyfinance.data.TransactionDao
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    navController: NavController,
    transactionDao: TransactionDao,
    modifier: Modifier = Modifier
) {
    val transactions by transactionDao.getAllTransactions().collectAsState(initial = emptyList())

    val incomeTotals = transactions
        .filter { it.isIncome }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }

    val expenseTotals = transactions
        .filter { !it.isIncome }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }

    val (quincenaLabels, incomesQuincenales, expensesQuincenales) = remember(transactions) {
        calculateQuincenalTotals(transactions)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Gráficos") }
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            PieChartView(dataMap = incomeTotals)

            Text(
                text = "Distribución de Gastos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            PieChartView(dataMap = expenseTotals)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Comparación Quincenal Gastos vs Ingresos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
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
        return transactions.filter { t ->
            val tDateMillis = t.startDate ?: return@filter false
            val tDate = Date(tDateMillis)
            !tDate.before(start.time) && tDate.before(end.time) && t.isIncome == isIncome
        }.sumOf { it.amount.toDouble() }.toFloat()
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
