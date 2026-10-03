package com.jiyibi.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyibi.app.data.CustomCategory
import com.jiyibi.app.data.TransactionType
import com.jiyibi.app.data.categoryColorOptions
import com.jiyibi.app.data.categoryIconOptions
import com.jiyibi.app.data.toCategory
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: TransactionViewModel, onBack: () -> Unit) {
    val customCategories by vm.customCategories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<CustomCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自定义分类") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加分类")
            }
        }
    ) { padding ->
        if (customCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有自定义分类\n点右下角 + 创建属于你的分类",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customCategories, key = { it.key }) { custom ->
                    val category = custom.toCategory()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(category.label, fontWeight = FontWeight.Medium)
                                Text(
                                    if (custom.type == TransactionType.EXPENSE.name) "支出" else "收入",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { toDelete = custom }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var label by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(TransactionType.EXPENSE) }
        var iconName by remember { mutableStateOf("more") }
        var colorArgb by remember { mutableIntStateOf(categoryColorOptions.first().toInt()) }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("新建分类") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { if (it.length <= 6) label = it },
                        label = { Text("分类名称（最多6字）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    Text("选择图标", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(categoryIconOptions.entries.toList(), key = { it.key }) { (name, icon) ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { iconName = name }
                                    .background(
                                        if (iconName == name) Color(colorArgb).copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = name, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Text("选择颜色", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoryColorOptions, key = { it }) { argb ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { colorArgb = argb.toInt() }
                                    .background(Color(argb))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorArgb == argb.toInt()) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.addCustomCategory(label.trim(), type, iconName, colorArgb)
                        showAdd = false
                    },
                    enabled = label.trim().isNotEmpty()
                ) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } }
        )
    }

    toDelete?.let { custom ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除这个分类？") },
            text = { Text("「${custom.label}」将被删除，已用该分类记的账会显示为「其他」") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCustomCategory(custom); toDelete = null }) {
                    Text("删除", color = ExpenseRed)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}
