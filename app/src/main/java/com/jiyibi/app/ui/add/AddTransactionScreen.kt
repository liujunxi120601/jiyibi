package com.jiyibi.app.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jiyibi.app.data.Category
import com.jiyibi.app.data.TransactionType
import com.jiyibi.app.data.allExpenseCategories
import com.jiyibi.app.data.allIncomeCategories
import com.jiyibi.app.data.transferCategory
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.components.formatDateFull
import com.jiyibi.app.ui.components.rememberBitmapFromFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 把图片复制到 App 私有目录，返回绝对路径 */
private fun copyImageToPrivateDir(context: android.content.Context, uri: Uri): String? {
    return try {
        val dir = File(context.getExternalFilesDir(null), "photos").apply { mkdirs() }
        val dest = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.absolutePath
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    vm: TransactionViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customCategories by vm.customCategories.collectAsState()

    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    val categories: List<Category> = when (type) {
        TransactionType.EXPENSE -> allExpenseCategories(customCategories)
        TransactionType.INCOME -> allIncomeCategories(customCategories)
        TransactionType.TRANSFER -> listOf(transferCategory)
    }
    var selectedCategoryKey by remember(type, customCategories) {
        mutableStateOf(categories.first().key)
    }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    // 转账
    var transferFrom by remember { mutableStateOf("") }
    var transferTo by remember { mutableStateOf("") }
    // 小票照片
    var photoPath by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val path = copyImageToPrivateDir(context, it)
                withContext(Dispatchers.Main) {
                    if (path != null) photoPath = path
                    else Toast.makeText(context, "图片保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val takePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                scope.launch(Dispatchers.IO) {
                    val path = copyImageToPrivateDir(context, uri)
                    withContext(Dispatchers.Main) {
                        if (path != null) photoPath = path
                    }
                }
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(
                context.getExternalFilesDir(null),
                "photos/camera_${System.currentTimeMillis()}.jpg"
            ).apply { parentFile?.mkdirs() }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            pendingCameraUri = uri
            takePhotoLauncher.launch(uri)
        } else {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val file = File(
                context.getExternalFilesDir(null),
                "photos/camera_${System.currentTimeMillis()}.jpg"
            ).apply { parentFile?.mkdirs() }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            pendingCameraUri = uri
            takePhotoLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0 }
    val isTransfer = type == TransactionType.TRANSFER
    val canSave = amount != null &&
        (!isTransfer || (transferFrom.isNotBlank() && transferTo.isNotBlank()))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记一笔") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 支出 / 收入 / 转账切换
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    label = { Text("支出") }
                )
                SegmentedButton(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    label = { Text("收入") }
                )
                SegmentedButton(
                    selected = type == TransactionType.TRANSFER,
                    onClick = { type = TransactionType.TRANSFER },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    label = { Text("转账") }
                )
            }

            // 金额输入
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = input
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    placeholder = { Text("0.00") },
                    prefix = { Text("¥ ", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                    textStyle = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (isTransfer) {
                // 转账双方
                OutlinedTextField(
                    value = transferFrom,
                    onValueChange = { transferFrom = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("转出方，如：微信零钱") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = transferTo,
                    onValueChange = { transferTo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("转入方，如：银行卡") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Text(
                    "转账不计入收支统计，只做记录",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 分类选择
                Text("分类", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.key }) { category ->
                        val selected = category.key == selectedCategoryKey
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedCategoryKey = category.key }
                                .background(
                                    if (selected) category.color.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) category.color
                                        else category.color.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    category.icon,
                                    contentDescription = category.label,
                                    tint = if (selected) MaterialTheme.colorScheme.surface
                                    else category.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(category.label, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 小票拍照
            Text("小票（可选）", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (photoPath.isNotBlank()) {
                val bitmap = rememberBitmapFromFile(photoPath)
                Box {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "小票照片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    IconButton(
                        onClick = { photoPath = "" },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                CircleShape
                            )
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "移除照片")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("拍照")
                    }
                    OutlinedButton(
                        onClick = {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("从相册选")
                    }
                }
            }

            // 日期选择
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(formatDateFull(dateMillis))
            }

            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("备注（可选）") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // 保存按钮
            Button(
                onClick = {
                    amount?.let {
                        vm.addTransaction(
                            type = type,
                            amount = it,
                            categoryKey = selectedCategoryKey,
                            timestamp = dateMillis,
                            note = note.trim(),
                            photoPath = photoPath,
                            transferFrom = transferFrom.trim(),
                            transferTo = transferTo.trim()
                        )
                        onSaved()
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("保存", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

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
