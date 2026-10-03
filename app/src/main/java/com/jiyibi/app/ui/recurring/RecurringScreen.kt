package com.jiyibi.app.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.jiyibi.app.data.RecurringTransaction
import com.jiyibi.app.data.TransactionType
import com.jiyibi.app.data.allExpenseCategories
import com.jiyibi.app.data.allIncomeCategories
import com.jiyibi.app.data.findCategory
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.components.formatDateFull
import com.jiyibi.app.ui.components.formatMoney
import com.jiyibi.app.ui.theme.ExpenseRed
import com.jiyibi.app.ui.theme.IncomeGreen

private val intervalOptions = listOf(1 to "每天", 7 to "每周", 30 to "每月")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(vm: TransactionViewModel, onBack: () -> Unit) {
    val list by vm.recurringList.collectAsState()
    val customCategories by vm.customCategories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<RecurringTransaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("周期性账目") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有周期性账目\n话费、会员订阅这种定期支出\n点右下角 + 添加，App 会自动记账",
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
                items(list, key = { it.id }) { item ->
                    val category = findCategory(item.categoryKey, customCategories)
                    val isIncome = item.type == TransactionType.INCOME.name
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(category.color.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(category.icon, contentDescription = category.label,
                                    tint = category.color, modifier = Modifier.size(24.dp))
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    (if (item.note.isNotBlank()) item.note else category.label),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${intervalOptions.find { it.first == item.intervalDays }?.second ?: "每${item.intervalDays}天"} · 下次 ${formatDateFull(item.nextDue)}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    (if (isIncome) "+" else "-") + formatMoney(item.amount),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) IncomeGreen else ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Switch(
                                    checked = item.active,
                                    onCheckedChange = { vm.setRecurringActive(item, it) }
                                )
                                IconButton(onClick = { toDelete = item }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        RecurringEditDialog(
            customCategories = customCategories,
            onDismiss = { showAdd = false },
            onSave = { amount, type, categoryKey, note, intervalDays, firstDue ->
                vm.addRecurring(amount, type, categoryKey, note, intervalDays, firstDue)
                showAdd = false
            }
        )
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除这条周期性账目？") },
            text = { Text("之后将不再自动记账") },
            confirmButton = {
                TextButton(onClick = { vm.deleteRecurring(item); toDelete = null }) {
                    Text("删除", color = ExpenseRed)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEditDialog(
    customCategories: List<com.jiyibi.app.data.CustomCategory>,
    onDismiss: () -> Unit,
    onSave: (Double, TransactionType, String, String, Int, Long) -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    val categories = if (type == TransactionType.EXPENSE)
        allExpenseCategories(customCategories) else allIncomeCategories(customCategories)
    var selectedKey by remember(type) { mutableStateOf(categories.first().key) }
    var note by remember { mutableStateOf("") }
    var intervalDays by remember { mutableIntStateOf(30) }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加周期性账目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("支出") }
                    )
                    SegmentedButton(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("收入") }
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = input
                    },
                    label = { Text("金额（元）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(150.dp),
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    intervalOptions.forEach { (days, label) ->
                        FilterChip(
                            selected = intervalDays == days,
                            onClick = { intervalDays = days },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Text("  首次记账：${formatDateFull(dateMillis)}")
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amount?.let { onSave(it, type, selectedKey, note.trim(), intervalDays, dateMillis) }
                },
                enabled = amount != null
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
