package com.jiyibi.app.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 金额格式化：整数不显示小数位 */
fun formatMoney(amount: Double): String =
    if (amount == kotlin.math.floor(amount)) {
        "¥${amount.toLong()}"
    } else {
        "¥${String.format(Locale.CHINA, "%.2f", amount)}"
    }

fun formatDateShort(timestamp: Long): String =
    SimpleDateFormat("M月d日", Locale.CHINA).format(Date(timestamp))

fun formatDateFull(timestamp: Long): String =
    SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(timestamp))
