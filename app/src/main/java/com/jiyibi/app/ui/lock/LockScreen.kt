package com.jiyibi.app.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyibi.app.util.AppLock

/**
 * 应用锁屏。
 * @param mode "verify" 验证解锁 / "setup" 设置新 PIN / "change" 修改 PIN（需先验证旧 PIN）
 */
@Composable
fun LockScreen(
    mode: String = "verify",
    onUnlock: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(if (mode == "setup") 1 else 0) }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when (mode) {
        "setup" -> "设置应用锁"
        "change" -> if (step == 0) "验证旧 PIN" else "设置新 PIN"
        else -> "记一笔已锁定"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                mode == "setup" && step == 1 -> "请设置 4-6 位数字 PIN 码"
                mode == "setup" && step == 2 -> "请再次输入确认"
                mode == "change" && step == 0 -> "请输入当前 PIN 码"
                mode == "change" -> "请输入新的 4-6 位 PIN 码"
                else -> "请输入 PIN 码解锁"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = if (step == 2) pinConfirm else pin,
            onValueChange = { input ->
                error = null
                if (input.length <= 6 && input.all { it.isDigit() }) {
                    if (step == 2) pinConfirm = input else pin = input
                }
            },
            label = { Text("PIN 码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            supportingText = error?.let { { Text(it) } }
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                when {
                    mode == "setup" && step == 1 -> {
                        if (pin.length < 4) error = "PIN 码至少 4 位"
                        else step = 2
                    }
                    mode == "setup" && step == 2 -> {
                        if (pinConfirm == pin) {
                            AppLock.setPin(context, pin)
                            onUnlock()
                        } else error = "两次输入不一致"
                    }
                    mode == "change" && step == 0 -> {
                        if (AppLock.verify(context, pin)) {
                            pin = ""; step = 1
                        } else error = "PIN 码错误"
                    }
                    mode == "change" -> {
                        if (pin.length < 4) error = "PIN 码至少 4 位"
                        else {
                            AppLock.setPin(context, pin)
                            onUnlock()
                        }
                    }
                    else -> {
                        if (AppLock.verify(context, pin)) onUnlock()
                        else error = "PIN 码错误"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = (if (step == 2) pinConfirm else pin).isNotEmpty()
        ) {
            Text(if (mode == "verify") "解锁" else "确定")
        }

        if (onCancel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onCancel) { Text("取消") }
            }
        }
    }
}
