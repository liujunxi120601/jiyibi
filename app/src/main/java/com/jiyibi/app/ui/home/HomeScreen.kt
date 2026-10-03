package com.jiyibi.app.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyibi.app.data.Transaction
import com.jiyibi.app.data.TransactionType
import com.jiyibi.app.data.findCategory
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.components.TransactionRow
import com.jiyibi.app.ui.components.formatMoney
import com.jiyibi.app.ui.theme.ExpenseRed
import com.jiyibi.app.ui.theme.IncomeGreen

@Composable
fun HomeScreen(
    vm: TransactionViewModel,
    onSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary by vm.monthSummary.collectAsState()
    val transactions by vm.monthTransactions.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val customCategories by vm.customCategories.collectAsState()
    var toDelete by remember { mutableStateOf<Transaction?>(null) }

    // 本月各分类已花费（预算进度用）
    val spentByCategory = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { it.categoryKey }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题栏 + 搜索入口
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("记一笔", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 本月收支概览卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "${vm.monthLabel()}结余",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatMoney(summary.balance),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("本月收入", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(formatMoney(summary.income), fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold, color = IncomeGreen)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("本月支出", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(formatMoney(summary.expense), fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                    }
                }
            }
        }

        // 预算进度（仅当设置了预算时显示）
        if (budgets.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("本月预算", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    budgets.forEach { budget ->
                        val category = findCategory(budget.categoryKey, customCategories)
                        val spent = spentByCategory[budget.categoryKey] ?: 0.0
                        val progress = (spent / budget.amount).toFloat().coerceIn(0f, 1f)
                        val over = spent > budget.amount
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(category.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${formatMoney(spent)} / ${formatMoney(budget.amount)}",
                                    fontSize = 13.sp,
                                    color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (over) ExpenseRed else category.color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            if (over) {
                                Text(
                                    "已超支 ${formatMoney(spent - budget.amount)}",
                                    fontSize = 12.sp,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }

        Text("最近记录", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有记录，点击右下角 + 记一笔吧",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions.take(30), key = { it.id }) { t ->
                    TransactionRow(
                        transaction = t,
                        customCategories = customCategories,
                        onDelete = { toDelete = t }
                    )
                }
            }
        }
    }

    toDelete?.let { t ->
        val cat = findCategory(t.categoryKey, customCategories)
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除这条记录？") },
            text = { Text("${cat.label}  ${formatMoney(t.amount)}") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTransaction(t); toDelete = null }) {
                    Text("删除", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("取消") }
            }
        )
    }
}
