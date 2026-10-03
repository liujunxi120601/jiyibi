package com.jiyibi.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionType { EXPENSE, INCOME, TRANSFER }

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

/** 转账专用分类（不计入收支统计） */
val transferCategory =
    Category("transfer", "转账", Icons.Filled.SwapHoriz, Color(0xFF64B5F6), TransactionType.TRANSFER)

/** 自定义分类可选图标 */
val categoryIconOptions: Map<String, ImageVector> = mapOf(
    "restaurant" to Icons.Filled.Restaurant,
    "bus" to Icons.Filled.DirectionsBus,
    "cart" to Icons.Filled.ShoppingCart,
    "home" to Icons.Filled.Home,
    "movie" to Icons.Filled.Movie,
    "hospital" to Icons.Filled.LocalHospital,
    "school" to Icons.Filled.School,
    "money" to Icons.Filled.AttachMoney,
    "gift" to Icons.Filled.CardGiftcard,
    "work" to Icons.Filled.Work,
    "flight" to Icons.Filled.Flight,
    "coffee" to Icons.Filled.Coffee,
    "pets" to Icons.Filled.Pets,
    "game" to Icons.Filled.SportsEsports,
    "phone" to Icons.Filled.PhoneAndroid,
    "more" to Icons.Filled.MoreHoriz,
)

/** 自定义分类可选颜色（ARGB） */
val categoryColorOptions = listOf(
    0xFFFF8A65, 0xFF64B5F6, 0xFFBA68C8, 0xFF4DB6AC,
    0xFFF06292, 0xFFE57373, 0xFF7986CB, 0xFF81C784,
    0xFFFFD54F, 0xFF4DD0E1, 0xFFA1887F, 0xFF90A4AE,
)

fun CustomCategory.toCategory(): Category = Category(
    key = key,
    label = label,
    icon = categoryIconOptions[iconName] ?: Icons.Filled.MoreHoriz,
    color = Color(colorArgb),
    type = TransactionType.valueOf(type)
)

fun allExpenseCategories(custom: List<CustomCategory>): List<Category> =
    expenseCategories + custom.filter { it.type == TransactionType.EXPENSE.name }.map { it.toCategory() }

fun allIncomeCategories(custom: List<CustomCategory>): List<Category> =
    incomeCategories + custom.filter { it.type == TransactionType.INCOME.name }.map { it.toCategory() }

fun findCategory(key: String, custom: List<CustomCategory> = emptyList()): Category {
    if (key == transferCategory.key) return transferCategory
    (expenseCategories + incomeCategories).find { it.key == key }?.let { return it }
    custom.find { it.key == key }?.let { return it.toCategory() }
    return Category("other_expense", "其他", Icons.Filled.MoreHoriz, Color(0xFF90A4AE), TransactionType.EXPENSE)
}
