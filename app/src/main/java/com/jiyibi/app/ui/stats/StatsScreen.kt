package com.jiyibi.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyibi.app.data.TransactionType
import com.jiyibi.app.data.findCategory
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.components.DonutChart
import com.jiyibi.app.ui.components.MonthBar
import com.jiyibi.app.ui.components.MonthlyBarChart
import com.jiyibi.app.ui.components.formatMoney
import java.util.Calendar

@Composable
fun StatsScreen(vm: TransactionViewModel) {
    val monthOffset by vm.monthOffset.collectAsState()
    val monthTransactions by vm.monthTransactions.collectAsState()
    val allTransactions by vm.allTransactions.collectAsState()

    // 本月支出按分类汇总（饼图用）
    val expenseByCategory = remember(monthTransactions) {
        monthTransactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { it.categoryKey }
            .map { (key, list) -> findCategory(key) to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }
    val expenseTotal = expenseByCategory.sumOf { it.second }

    // 近 6 个月收支（柱状图用）
    val last6Months = remember(allTransactions) {
        (5 downTo 0).map { back ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -back)
            val label = "${cal.get(Calendar.MONTH) + 1}月"
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            val inRange = allTransactions.filter { it.timestamp in start until end }
            MonthBar(
                label = label,
                income = inRange.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount },
                expense = inRange.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 月份切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.prevMonth() }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "上个月")
            }
            Text(vm.monthLabel(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = { vm.nextMonth() },
                enabled = monthOffset < 0
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "下个月")
            }
        }

        // 支出分类饼图
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("支出分类", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "共支出 ${formatMoney(expenseTotal)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                if (expenseByCategory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("本月暂无支出记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            data = expenseByCategory,
                            modifier = Modifier.size(190.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("总支出", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatMoney(expenseTotal), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    expenseByCategory.forEach { (category, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(category.color, radius = size.minDimension / 2f)
                            }
                            Text(
                                category.label,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                fontSize = 14.sp
                            )
                            Text(
                                "${(value / expenseTotal * 100).toInt()}%",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatMoney(value),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 近6个月收支柱状图
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("近6个月收支", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(16.dp))
                MonthlyBarChart(data = last6Months, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
