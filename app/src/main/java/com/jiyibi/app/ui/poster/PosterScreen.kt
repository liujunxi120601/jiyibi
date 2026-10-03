package com.jiyibi.app.ui.poster

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jiyibi.app.data.CustomCategory
import com.jiyibi.app.data.Transaction
import com.jiyibi.app.data.TransactionType
import com.jiyibi.app.data.findCategory
import com.jiyibi.app.ui.TransactionViewModel
import com.jiyibi.app.ui.components.formatMoney
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/** 生成月度账单海报 Bitmap（1080 x 1560） */
fun generatePosterBitmap(
    context: Context,
    monthLabel: String,
    transactions: List<Transaction>,
    customCategories: List<CustomCategory>
): Bitmap {
    val w = 1080
    val h = 1560
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val density = context.resources.displayMetrics.density
    fun dp(v: Float) = v * density

    // 背景渐变
    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            (0xFF6D7CF5L).toInt(), (0xFF9B6BF3L).toInt(),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    // 白色卡片
    val cardPaint = Paint().apply { color = (0xFFFFFFFFL).toInt(); isAntiAlias = true }
    val cardRect = RectF(dp(24f), dp(200f), w - dp(24f), h - dp(80f))
    canvas.drawRoundRect(cardRect, dp(32f), dp(32f), cardPaint)

    fun text(
        s: String, x: Float, y: Float, sizeSp: Float, color: Int,
        bold: Boolean = false, align: Paint.Align = Paint.Align.CENTER
    ) {
        val p = Paint().apply {
            this.color = color
            textSize = dp(sizeSp)
            textAlign = align
            isAntiAlias = true
            if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(s, x, y, p)
    }

    // 顶部标题
    text("记一笔 · 月度账单", w / 2f, dp(110f), 30f, (0xFFFFFFFFL).toInt(), bold = true)
    text(monthLabel, w / 2f, dp(160f), 18f, (0xE0FFFFFFL).toInt())

    val income = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    val expense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    val balance = income - expense

    var y = dp(320f)
    text("本月结余", w / 2f, y, 20f, (0xFF888888L).toInt())
    y += dp(64f)
    text(formatMoney(balance), w / 2f, y, 56f, (0xFF222222L).toInt(), bold = true)
    y += dp(72f)

    // 收支两栏
    text("总收入", w * 0.3f, y, 18f, (0xFF888888L).toInt())
    text("总支出", w * 0.7f, y, 18f, (0xFF888888L).toInt())
    y += dp(48f)
    text(formatMoney(income), w * 0.3f, y, 30f, (0xFF4CAF50L).toInt(), bold = true)
    text(formatMoney(expense), w * 0.7f, y, 30f, (0xFFE57373L).toInt(), bold = true)
    y += dp(80f)

    // 分隔线
    val linePaint = Paint().apply { color = (0xFFEEEEEEL).toInt(); strokeWidth = dp(2f) }
    canvas.drawLine(dp(64f), y, w - dp(64f), y, linePaint)
    y += dp(56f)

    // 支出 Top 5
    text("支出 Top 5", w / 2f, y, 22f, (0xFF222222L).toInt(), bold = true)
    y += dp(48f)

    val top5 = transactions
        .filter { it.type == TransactionType.EXPENSE.name }
        .groupBy { it.categoryKey }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }.take(5)
    val maxTop = top5.firstOrNull()?.value ?: 1.0

    val barBg = Paint().apply { color = (0xFFF0F0F0L).toInt(); isAntiAlias = true }
    top5.forEach { (key, amount) ->
        val category = findCategory(key, customCategories)
        text(category.label, dp(96f), y, 18f, (0xFF555555L).toInt(), align = Paint.Align.LEFT)
        text(formatMoney(amount), w - dp(96f), y, 18f, (0xFF222222L).toInt(),
            bold = true, align = Paint.Align.RIGHT)
        y += dp(28f)
        val barW = (w - dp(192f)) * (amount / maxTop).toFloat()
        val barRect = RectF(dp(96f), y, dp(96f) + barW.coerceAtLeast(dp(8f)), y + dp(16f))
        canvas.drawRoundRect(
            RectF(dp(96f), y, w - dp(96f), y + dp(16f)), dp(8f), dp(8f), barBg
        )
        val barPaint = Paint().apply {
            color = (0xFF000000L or category.color.value.toLong()).toInt()
            isAntiAlias = true
        }
        canvas.drawRoundRect(barRect, dp(8f), dp(8f), barPaint)
        y += dp(56f)
    }
    if (top5.isEmpty()) {
        text("本月还没有支出记录", w / 2f, y, 18f, (0xFFAAAAAAL).toInt())
        y += dp(56f)
    }

    // 底部
    text("共 ${transactions.size} 笔记录", w / 2f, h - dp(160f), 16f, (0xFFAAAAAAL).toInt())
    text("记一笔 · 每一笔都算数", w / 2f, h - dp(120f), 16f, (0xFFAAAAAAL).toInt())

    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosterScreen(vm: TransactionViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val transactions by vm.monthTransactions.collectAsState()
    val customCategories by vm.customCategories.collectAsState()
    val monthLabel = vm.monthLabel()

    val bitmap by produceState<Bitmap?>(null, transactions, customCategories) {
        value = generatePosterBitmap(context, monthLabel, transactions, customCategories)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("月度账单卡片") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val bmp = bitmap ?: return@ExtendedFloatingActionButton
                    val file = File(context.cacheDir, "jiyibi_poster_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "分享月度账单"))
                },
                icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                text = { Text("分享卡片") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "月度账单卡片预览",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                )
            } else {
                Text("正在生成…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
