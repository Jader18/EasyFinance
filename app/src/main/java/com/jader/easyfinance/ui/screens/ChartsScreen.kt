package com.jader.easyfinance.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.jader.easyfinance.data.TransactionDao
import androidx.compose.ui.graphics.Color

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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Distribución de Ingresos",
                style = MaterialTheme.typography.titleMedium
            )
            PieChartView(dataMap = incomeTotals)

            Text(
                text = "Distribución de Gastos",
                style = MaterialTheme.typography.titleMedium
            )
            PieChartView(dataMap = expenseTotals)
        }
    }
}

@Composable
fun PieChartView(dataMap: Map<String, Float>, modifier: Modifier = Modifier) {
    val entries = dataMap.map { PieEntry(it.value, it.key) }
    val colors = listOf(
        Color(0xFF4CAF50).toArgb(), // Verde
        Color(0xFFF44336).toArgb(), // Rojo
        Color(0xFF2196F3).toArgb(), // Azul
        Color(0xFFFFC107).toArgb(), // Amarillo
        Color(0xFF9C27B0).toArgb(), // Morado
        Color(0xFF009688).toArgb()  // Teal
    )
    val legendTextColor = MaterialTheme.colorScheme.onBackground.toArgb()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        factory = { context ->
            PieChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                description.isEnabled = false
                setUsePercentValues(true)
                setDrawEntryLabels(true)
                setEntryLabelTextSize(14f)
                setEntryLabelColor(android.graphics.Color.BLACK)

                legend.orientation = Legend.LegendOrientation.VERTICAL
                legend.isWordWrapEnabled = true
                legend.setDrawInside(false)
                legend.textSize = 14f
                legend.textColor = legendTextColor

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
                setValueFormatter(com.github.mikephil.charting.formatter.PercentFormatter(pieChart))
            }
            pieChart.data = PieData(dataSet)
            pieChart.invalidate()
        }
    )
}


