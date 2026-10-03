package com.jiyibi.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionType { EXPENSE, INCOME }

data class Category(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val type: TransactionType
)

val expenseCategories = listOf(
    Category("food", "餐饮", Icons.Filled.Restaurant, Color(0xFFFF8A65), TransactionType.EXPENSE),
    Category("transport", "交通", Icons.Filled.DirectionsBus, Color(0xFF64B5F6), TransactionType.EXPENSE),
    Category("shopping", "购物", Icons.Filled.ShoppingCart, Color(0xFFBA68C8), TransactionType.EXPENSE),
    Category("housing", "住房", Icons.Filled.Home, Color(0xFF4DB6AC), TransactionType.EXPENSE),
    Category("entertainment", "娱乐", Icons.Filled.Movie, Color(0xFFF06292), TransactionType.EXPENSE),
    Category("medical", "医疗", Icons.Filled.LocalHospital, Color(0xFFE57373), TransactionType.EXPENSE),
    Category("education", "教育", Icons.Filled.School, Color(0xFF7986CB), TransactionType.EXPENSE),
    Category("other_expense", "其他", Icons.Filled.MoreHoriz, Color(0xFF90A4AE), TransactionType.EXPENSE),
)

val incomeCategories = listOf(
    Category("salary", "工资", Icons.Filled.AttachMoney, Color(0xFF81C784), TransactionType.INCOME),
    Category("bonus", "奖金", Icons.Filled.CardGiftcard, Color(0xFFFFD54F), TransactionType.INCOME),
    Category("parttime", "兼职", Icons.Filled.Work, Color(0xFF4DD0E1), TransactionType.INCOME),
    Category("other_income", "其他", Icons.Filled.MoreHoriz, Color(0xFF90A4AE), TransactionType.INCOME),
)

fun findCategory(key: String): Category =
    (expenseCategories + incomeCategories).find { it.key == key }
        ?: Category("other_expense", "其他", Icons.Filled.MoreHoriz, Color(0xFF90A4AE), TransactionType.EXPENSE)
