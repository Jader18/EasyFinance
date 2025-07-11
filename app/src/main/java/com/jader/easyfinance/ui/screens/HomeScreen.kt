package com.jader.easyfinance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jader.easyfinance.data.TransactionDao
import com.jader.easyfinance.ui.theme.EasyFinanceTheme
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    transactionDao: TransactionDao,
    modifier: Modifier = Modifier
) {
    val transactions by transactionDao.getAllTransactions().collectAsState(initial = emptyList())
    val totalIncomes = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpenses = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncomes - totalExpenses
    val decimalFormat = DecimalFormat("C$ #,##0.00")

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Easy Finance") }
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
            Text(
                text = "Balance: ${decimalFormat.format(balance)}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { navController.navigate("add_transaction") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar Transacción")
            }
            Button(
                onClick = { navController.navigate("transactions") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver Transacciones")
            }
            Button(
                onClick = { navController.navigate("recurring_transactions") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver Transacciones Recurrentes")
            }
        }
    }
}