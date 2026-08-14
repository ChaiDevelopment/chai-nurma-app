@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chai.nurma.app

// File BARU: layar Laporan (tab ke-3). Tidak menyentuh FinanceScreen / CoupleScreen yang sudah ada.
// Menggunakan StatCard, warna, dan style yang sama dengan MainActivity.kt.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chai.nurma.app.data.ApiClient
import com.chai.nurma.app.data.CashflowPointDto
import com.chai.nurma.app.data.MonthlyReportDto
import com.chai.nurma.app.data.UserDto
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private val monthNames = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

@Composable
fun ReportScreen(user: UserDto) {
    val cal = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) } // 1-12
    var scope by remember { mutableStateOf("both") } // "me" | "partner" | "both"

    var report by remember { mutableStateOf<MonthlyReportDto?>(null) }
    var cashflow by remember { mutableStateOf<List<CashflowPointDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val money = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    fun refresh() {
        coroutineScope.launch {
            loading = true
            try {
                report = ApiClient.api.monthlyReport(user.id, year, month, scope)
                cashflow = ApiClient.api.cashflow(user.id, scope, 6)
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Gagal memuat laporan"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(year, month, scope) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Laporan Keuangan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))

        // Navigasi bulan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (month == 1) { month = 12; year -= 1 } else { month -= 1 }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
            }
            Text(
                "${monthNames.getOrElse(month - 1) { "" }} $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = {
                if (month == 12) { month = 1; year += 1 } else { month += 1 }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Filter Saya / Pasangan / Bersama
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = scope == "me", onClick = { scope = "me" }, label = { Text("Saya") })
            FilterChip(selected = scope == "partner", onClick = { scope = "partner" }, label = { Text("Pasangan") })
            FilterChip(selected = scope == "both", onClick = { scope = "both" }, label = { Text("Bersama") })
        }

        Spacer(Modifier.height(16.dp))

        if (loading && report == null) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        report?.let { r ->
            // Total saldo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(BrandPurple, BrandPurpleDark)))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Total Saldo", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        money.format(r.totalBalance),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    title = "Pemasukan",
                    value = money.format(r.totalIncome),
                    icon = Icons.Filled.ArrowUpward,
                    accentColor = IncomeGreen,
                    accentBg = IncomeGreenBg,
                    selected = false,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pengeluaran",
                    value = money.format(r.totalExpense),
                    icon = Icons.Filled.ArrowDownward,
                    accentColor = ExpenseRed,
                    accentBg = ExpenseRedBg,
                    selected = false,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Net Cash Flow
            val positive = r.netCashFlow >= 0
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (positive) IncomeGreenBg else ExpenseRedBg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (positive) "Cashflow positif" else "Pengeluaran melebihi pemasukan",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (positive) IncomeGreen else ExpenseRed
                        )
                        Text(
                            "${if (positive) "+" else ""}${money.format(r.netCashFlow)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (positive) IncomeGreen else ExpenseRed
                        )
                    }
                    Text("${r.transactionCount} transaksi", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Cash Flow Bulanan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Perbandingan pemasukan dan pengeluaran 6 bulan terakhir",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                CashflowLineChart(
                    points = cashflow,
                    incomeColor = IncomeGreen,
                    expenseColor = ExpenseRed,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text("Pengeluaran per Kategori", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                ExpenseCategoryBarChart(
                    data = r.expenseByCategory,
                    barColor = BrandPurple,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
