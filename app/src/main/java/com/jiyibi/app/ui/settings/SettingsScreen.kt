package com.jiyibi.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jiyibi.app.reminder.ReminderScheduler
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.theme.ExpenseRed
import com.jiyibi.app.util.AppLock
import com.jiyibi.app.util.ReminderPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: TransactionViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onLockSetup: () -> Unit,
    onLockChange: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reminderEnabled by remember { mutableStateOf(ReminderPrefs.isEnabled(context)) }
    var lockEnabled by remember { mutableStateOf(AppLock.isEnabled(context)) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun doExportBackup(uri: android.net.Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = vm.exportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "备份已保存", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "备份失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun doExportCsv(uri: android.net.Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                val csv = "\uFEFF" + vm.exportCsv() // BOM，Excel 打开不乱码
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "CSV 已导出", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun doRestore(uri: android.net.Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: throw IllegalStateException("无法读取文件")
                vm.restoreBackupJson(json)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "恢复成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "恢复失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { doExportBackup(it) } }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { doExportCsv(it) } }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            showRestoreConfirm = true
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReminderScheduler.schedule(context)
            ReminderPrefs.setEnabled(context, true)
            reminderEnabled = true
        } else {
            Toast.makeText(context, "需要通知权限才能提醒记账", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsGroup(title = "数据管理") {
                SettingsRow(
                    title = "备份数据",
                    subtitle = "导出 JSON 备份文件",
                    onClick = { backupLauncher.launch("jiyibi_backup.json") }
                )
                SettingsRow(
                    title = "恢复数据",
                    subtitle = "从备份文件恢复（会覆盖当前数据）",
                    titleColor = ExpenseRed,
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) }
                )
                SettingsRow(
                    title = "导出 CSV",
                    subtitle = "可用 Excel 打开",
                    onClick = { csvLauncher.launch("jiyibi.csv") }
                )
            }

            SettingsGroup(title = "记账工具") {
                SettingsRow(
                    title = "月度预算",
                    subtitle = "为分类设置每月 spending 上限",
                    onClick = { onNavigate("budgets") }
                )
                SettingsRow(
                    title = "储蓄目标",
                    subtitle = "存钱罐，攒钱更有动力",
                    onClick = { onNavigate("goals") }
                )
                SettingsRow(
                    title = "周期性账目",
                    subtitle = "话费、订阅等自动记账",
                    onClick = { onNavigate("recurring") }
                )
                SettingsRow(
                    title = "自定义分类",
                    subtitle = "创建属于你的分类",
                    onClick = { onNavigate("categories") }
                )
                SettingsRow(
                    title = "月度账单卡片",
                    subtitle = "生成海报分享本月账单",
                    onClick = { onNavigate("poster") }
                )
            }

            SettingsGroup(title = "提醒与安全") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("每日记账提醒", fontWeight = FontWeight.Medium)
                        Text(
                            "每天 21:00 提醒记账",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    ReminderScheduler.schedule(context)
                                    ReminderPrefs.setEnabled(context, true)
                                    reminderEnabled = true
                                }
                            } else {
                                ReminderScheduler.cancel(context)
                                ReminderPrefs.setEnabled(context, false)
                                reminderEnabled = false
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (lockEnabled) onLockChange() else onLockSetup()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text("应用锁", fontWeight = FontWeight.Medium)
                        Text(
                            if (lockEnabled) "已开启，点击修改 PIN 码" else "未开启，点击设置 PIN 码",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (lockEnabled) {
                    var showDisableConfirm by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDisableConfirm = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("关闭应用锁", color = ExpenseRed, fontWeight = FontWeight.Medium)
                    }
                    if (showDisableConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDisableConfirm = false },
                            title = { Text("关闭应用锁？") },
                            text = { Text("之后打开 App 将不再需要 PIN 码") },
                            confirmButton = {
                                TextButton(onClick = {
                                    AppLock.clear(context)
                                    lockEnabled = false
                                    showDisableConfirm = false
                                }) { Text("关闭", color = ExpenseRed) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDisableConfirm = false }) { Text("取消") }
                            }
                        )
                    }
                }
            }

            SettingsGroup(title = "关于") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("版本", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(
                        "1.1",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; pendingRestoreUri = null },
            title = { Text("确认恢复？") },
            text = { Text("恢复将用备份文件覆盖当前全部数据（账单、预算、目标、分类等），此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri?.let { doRestore(it) }
                    showRestoreConfirm = false
                    pendingRestoreUri = null
                }) { Text("确认恢复", color = ExpenseRed) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false; pendingRestoreUri = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = titleColor)
            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
