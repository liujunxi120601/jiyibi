package com.jiyibi.app.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyibi.app.data.Budget
import com.jiyibi.app.data.TransactionType
import com.jiyibi.app.data.allExpenseCategories
import com.jiyibi.app.data.findCategory
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.components.formatMoney
import com.jiyibi.app.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(vm: TransactionViewModel, onBack: () -> Unit) {
    val budgets by vm.budgets.collectAsState()
    val transactions by vm.monthTransactions.collectAsState()
    val customCategories by vm.customCategories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Budget?>(null) }

    val spentByCategory = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { it.categoryKey }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${vm.monthLabel()}预算") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加预算")
            }
        }
    ) { padding ->
        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有设置预算\n点右下角 + 为分类设置月度预算吧",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets, key = { it.id }) { budget ->
                    val category = findCategory(budget.categoryKey, customCategories)
                    val spent = spentByCategory[budget.categoryKey] ?: 0.0
                    val progress = (spent / budget.amount).toFloat().coerceIn(0f, 1f)
                    val over = spent > budget.amount
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(category.color.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(category.icon, contentDescription = category.label,
                                        tint = category.color, modifier = Modifier.size(22.dp))
                                }
                                Text(
                                    category.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )
                                IconButton(onClick = { toDelete = budget }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除预算",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = if (over) ExpenseRed else category.color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "已用 ${formatMoney(spent)}",
                                    fontSize = 13.sp,
                                    color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    if (over) "超支 ${formatMoney(spent - budget.amount)}"
                                    else "剩余 ${formatMoney(budget.amount - spent)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "预算 ${formatMoney(budget.amount)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        BudgetEditDialog(
            customCategories = customCategories,
            existingKeys = budgets.map { it.categoryKey }.toSet(),
            onDismiss = { showAdd = false },
            onSave = { categoryKey, amount ->
                vm.upsertBudget(categoryKey, amount)
                showAdd = false
            }
        )
    }

    toDelete?.let { budget ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除这条预算？") },
            text = { Text("${findCategory(budget.categoryKey, customCategories).label} ${formatMoney(budget.amount)}") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteBudget(budget.categoryKey)
                    toDelete = null
                }) { Text("删除", color = ExpenseRed) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun BudgetEditDialog(
    customCategories: List<com.jiyibi.app.data.CustomCategory>,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onSave: (categoryKey: String, amount: Double) -> Unit
) {
    val categories = allExpenseCategories(customCategories).filter { it.key !in existingKeys }
    var selectedKey by remember { mutableStateOf(categories.firstOrNull()?.key ?: "") }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置月度预算") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (categories.isEmpty()) {
                    Text("所有支出分类都已设置预算", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories, key = { it.key }) { category ->
                            val selected = category.key == selectedKey
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedKey = category.key }
                                    .background(
                                        if (selected) category.color.copy(alpha = 0.22f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(vertical = 8.dp)
                            ) {
                                Icon(category.icon, contentDescription = category.label,
                                    tint = category.color, modifier = Modifier.size(22.dp))
                                Text(category.label, fontSize = 11.sp)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = input
                    },
                    label = { Text("预算金额（元）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let { onSave(selectedKey, it) } },
                enabled = selectedKey.isNotBlank() && amount != null
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
