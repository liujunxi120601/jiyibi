package com.jiyibi.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyibi.app.data.Transaction
import com.jiyibi.app.data.TransactionType
import com.jiyibi.app.data.allExpenseCategories
import com.jiyibi.app.data.allIncomeCategories
import com.jiyibi.app.data.findCategory
import com.jiyibi.app.ui.SearchFilter
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.components.TransactionRow
import com.jiyibi.app.ui.components.formatMoney
import com.jiyibi.app.ui.theme.ExpenseRed
import java.util.Calendar

private val typeOptions = listOf(
    "" to "全部",
    TransactionType.EXPENSE.name to "支出",
    TransactionType.INCOME.name to "收入",
    TransactionType.TRANSFER.name to "转账"
)

private val rangeOptions = listOf(
    "全部时间" to null as Pair<Long, Long>?,
    "近7天" to daysRange(7),
    "近30天" to daysRange(30),
    "近90天" to daysRange(90)
)

private fun daysRange(days: Int): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    val end = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, -days)
    return cal.timeInMillis to end
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: TransactionViewModel, onBack: () -> Unit) {
    val filter by vm.searchFilter.collectAsState()
    val results by vm.searchResults.collectAsState()
    val customCategories by vm.customCategories.collectAsState()
    var toDelete by remember { mutableStateOf<Transaction?>(null) }

    val categories = remember(customCategories, filter.type) {
        when (filter.type) {
            TransactionType.EXPENSE.name -> allExpenseCategories(customCategories)
            TransactionType.INCOME.name -> allIncomeCategories(customCategories)
            else -> allExpenseCategories(customCategories) + allIncomeCategories(customCategories)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索账单") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 关键词
            OutlinedTextField(
                value = filter.keyword,
                onValueChange = { vm.setSearchFilter(filter.copy(keyword = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索备注、金额…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.keyword.isNotEmpty()) {
                        IconButton(onClick = { vm.setSearchFilter(filter.copy(keyword = "")) }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true
            )

            // 类型筛选
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(typeOptions) { (value, label) ->
                    FilterChip(
                        selected = filter.type == value,
                        onClick = { vm.setSearchFilter(filter.copy(type = value, categoryKey = "")) },
                        label = { Text(label) }
                    )
                }
            }

            // 分类筛选
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter.categoryKey == "",
                        onClick = { vm.setSearchFilter(filter.copy(categoryKey = "")) },
                        label = { Text("全部分类") }
                    )
                }
                items(categories, key = { it.key }) { category ->
                    FilterChip(
                        selected = filter.categoryKey == category.key,
                        onClick = { vm.setSearchFilter(filter.copy(categoryKey = category.key)) },
                        label = { Text(category.label) }
                    )
                }
            }

            // 时间筛选
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rangeOptions) { (label, range) ->
                    val selected = if (range == null) {
                        filter.from == 0L && filter.to == Long.MAX_VALUE
                    } else {
                        filter.from == range.first && filter.to == range.second
                    }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            vm.setSearchFilter(
                                if (range == null) filter.copy(from = 0L, to = Long.MAX_VALUE)
                                else filter.copy(from = range.first, to = range.second)
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "共 ${results.size} 条",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (results.isNotEmpty()) {
                    Text(
                        "  支出 ${formatMoney(results.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount })}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (filter.keyword.isBlank() && filter.type.isBlank() && filter.categoryKey.isBlank())
                            "输入关键词或选择筛选条件开始搜索"
                        else "没有符合条件的记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.id }) { t ->
                        TransactionRow(
                            transaction = t,
                            customCategories = customCategories,
                            onDelete = { toDelete = t }
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除这条记录？") },
            text = { Text("${findCategory(t.categoryKey, customCategories).label}  ${formatMoney(t.amount)}") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTransaction(t); toDelete = null }) {
                    Text("删除", color = ExpenseRed)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}
