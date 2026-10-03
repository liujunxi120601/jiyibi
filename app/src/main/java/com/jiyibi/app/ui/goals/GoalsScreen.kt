package com.jiyibi.app.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.jiyibi.app.data.SavingsGoal
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.components.formatDateFull
import com.jiyibi.app.ui.components.formatMoney
import com.jiyibi.app.ui.theme.ExpenseRed
import com.jiyibi.app.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(vm: TransactionViewModel, onBack: () -> Unit) {
    val goals by vm.savingsGoals.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var contributing by remember { mutableStateOf<SavingsGoal?>(null) }
    var toDelete by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("储蓄目标") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新建目标")
            }
        }
    ) { padding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有储蓄目标\n点右下角 + 建一个存钱罐吧",
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
                items(goals, key = { it.id }) { goal ->
                    val progress = (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                    val done = goal.savedAmount >= goal.targetAmount
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
                                Text(
                                    goal.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (done) {
                                    Text(
                                        "已达成 🎉",
                                        fontSize = 13.sp,
                                        color = IncomeGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = { toDelete = goal }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = IncomeGreen,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "已存 ${formatMoney(goal.savedAmount)}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "目标 ${formatMoney(goal.targetAmount)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (goal.deadline > 0) {
                                Text(
                                    "目标日期：${formatDateFull(goal.deadline)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { contributing = goal }) {
                                    Text("存入一笔")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("") }
        val target = targetText.toDoubleOrNull()?.takeIf { it > 0 }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("新建储蓄目标") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("目标名称，如：换新耳机") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { input ->
                            if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) targetText = input
                        },
                        label = { Text("目标金额（元）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        target?.let {
                            vm.addSavingsGoal(name.trim().ifBlank { "存钱罐" }, it)
                            showAdd = false
                        }
                    },
                    enabled = target != null
                ) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } }
        )
    }

    contributing?.let { goal ->
        var amountText by remember { mutableStateOf("") }
        val amount = amountText.toDoubleOrNull()?.takeIf { it > 0 }
        AlertDialog(
            onDismissRequest = { contributing = null },
            title = { Text("向「${goal.name}」存入") },
            text = {
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        amount?.let {
                            vm.contributeGoal(goal, it)
                            contributing = null
                        }
                    },
                    enabled = amount != null
                ) { Text("存入") }
            },
            dismissButton = { TextButton(onClick = { contributing = null }) { Text("取消") } }
        )
    }

    toDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除这个储蓄目标？") },
            text = { Text("「${goal.name}」及已存进度将被删除") },
            confirmButton = {
                TextButton(onClick = { vm.deleteGoal(goal); toDelete = null }) {
                    Text("删除", color = ExpenseRed)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}
