package com.chai.nurma.app

// File BARU: chart sederhana berbasis Canvas Compose, tidak menambah dependency baru,
// dan tidak menyentuh MainActivity.kt selain pemanggilannya di ReportScreen.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chai.nurma.app.data.CashflowPointDto
import com.chai.nurma.app.data.CategoryTotalDto
import java.text.NumberFormat
import java.util.Locale

private fun shortMonthLabel(period: String): String {
    // period format: "yyyy-MM"
    val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    return try {
        val idx = period.substring(5, 7).toInt() - 1
        months.getOrElse(idx) { period }
    } catch (e: Exception) {
        period
    }
}

/**
 * Line chart cash flow bulanan: dua garis (income & expense) berdasarkan data nyata dari backend.
 */
@Composable
fun CashflowLineChart(
    points: List<CashflowPointDto>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            LegendDot(incomeColor); Spacer(Modifier.width(4.dp)); Text("Pemasukan", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(12.dp))
            LegendDot(expenseColor); Spacer(Modifier.width(4.dp)); Text("Pengeluaran", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(8.dp))

        if (points.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Belum ada data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        val maxValue = (points.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0).let { if (it <= 0.0) 1.0 else it }

        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val leftPad = 8f
            val bottomPad = 20f
            val chartWidth = size.width - leftPad * 2
            val chartHeight = size.height - bottomPad

            fun xFor(i: Int) = leftPad + if (points.size == 1) chartWidth / 2 else chartWidth * i / (points.size - 1)
            fun yFor(v: Double) = (chartHeight - (v / maxValue * chartHeight)).toFloat()

            // grid line
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(leftPad, chartHeight),
                end = Offset(leftPad + chartWidth, chartHeight),
                strokeWidth = 2f
            )

            fun drawSeries(color: Color, valueOf: (CashflowPointDto) -> Double) {
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = color,
                        start = Offset(xFor(i), yFor(valueOf(points[i]))),
                        end = Offset(xFor(i + 1), yFor(valueOf(points[i + 1]))),
                        strokeWidth = 5f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                points.forEachIndexed { i, p ->
                    drawCircle(color = color, radius = 6f, center = Offset(xFor(i), yFor(valueOf(p))))
                }
            }

            drawSeries(incomeColor) { it.income }
            drawSeries(expenseColor) { it.expense }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { p ->
                Text(
                    shortMonthLabel(p.month),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Bar chart horizontal untuk pengeluaran per kategori, berdasarkan transactions.category_id.
 */
@Composable
fun ExpenseCategoryBarChart(
    data: List<CategoryTotalDto>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val money = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val sorted = data.sortedByDescending { it.total }.take(6)
    val maxValue = (sorted.maxOfOrNull { it.total } ?: 0.0).let { if (it <= 0.0) 1.0 else it }

    Column(modifier = modifier) {
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Belum ada pengeluaran bulan ini", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        sorted.forEach { item ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(money.format(item.total), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                    val fraction = (item.total / maxValue).toFloat().coerceIn(0f, 1f)
                    drawRoundRect(
                        color = barColor.copy(alpha = 0.15f),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = barColor,
                        size = size.copy(width = size.width * fraction),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(color = color)
    }
}
