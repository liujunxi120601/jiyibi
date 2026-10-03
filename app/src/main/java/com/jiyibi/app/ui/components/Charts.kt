package com.jiyibi.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyibi.app.data.Category
import com.jiyibi.app.ui.theme.ExpenseRed
import com.jiyibi.app.ui.theme.IncomeGreen

/**
 * 环形饼图：用 Canvas 自绘，无第三方依赖。
 */
@Composable
fun DonutChart(
    data: List<Pair<Category, Double>>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.second }.takeIf { it > 0 } ?: 1.0
    Canvas(modifier = modifier) {
        var startAngle = -90f
        val strokeWidth = size.minDimension / 3.2f
        data.forEach { (category, value) ->
            val sweep = (value / total * 360f).toFloat()
            if (sweep > 0f) {
                drawArc(
                    color = category.color,
                    startAngle = startAngle,
                    sweepAngle = sweep - 1.5f, // 留一点间隙更美观
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            }
            startAngle += sweep
        }
    }
}

data class MonthBar(val label: String, val income: Double, val expense: Double)

/**
 * 近 N 个月收支分组柱状图：Canvas 自绘。
 */
@Composable
fun MonthlyBarChart(
    data: List<MonthBar>,
    modifier: Modifier = Modifier
) {
    val maxValue = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0)
        .takeIf { it > 0 } ?: 1.0
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val groupWidth = size.width / data.size
            val barWidth = groupWidth * 0.26f
            val gap = 3.dp.toPx()
            data.forEachIndexed { index, bar ->
                val centerX = groupWidth * index + groupWidth / 2f
                val chartHeight = size.height
                val incomeH = (bar.income / maxValue * chartHeight * 0.92f).toFloat()
                val expenseH = (bar.expense / maxValue * chartHeight * 0.92f).toFloat()
                if (incomeH > 0f) {
                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(centerX - barWidth - gap, chartHeight - incomeH),
                        size = Size(barWidth, incomeH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
                if (expenseH > 0f) {
                    drawRoundRect(
                        color = ExpenseRed,
                        topLeft = Offset(centerX + gap, chartHeight - expenseH),
                        size = Size(barWidth, expenseH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { bar ->
                Text(
                    text = bar.label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(IncomeGreen, "收入")
            LegendDot(ExpenseRed, "支出", Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.padding(end = 4.dp).height(10.dp)) {
            drawCircle(color, radius = size.minDimension / 2f)
        }
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
