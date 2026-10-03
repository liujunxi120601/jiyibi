package com.jiyibi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyibi.app.data.CustomCategory
import com.jiyibi.app.data.Transaction
import com.jiyibi.app.data.TransactionType
import com.jiyibi.app.data.findCategory
import com.jiyibi.app.ui.theme.ExpenseRed
import com.jiyibi.app.ui.theme.IncomeGreen

@Composable
fun TransactionRow(
    transaction: Transaction,
    customCategories: List<CustomCategory> = emptyList(),
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = findCategory(transaction.categoryKey, customCategories)
    val isIncome = transaction.type == TransactionType.INCOME.name
    val isTransfer = transaction.type == TransactionType.TRANSFER.name

    val subtitle = when {
        isTransfer -> {
            val route = listOf(transaction.transferFrom, transaction.transferTo)
                .filter { it.isNotBlank() }.joinToString(" → ")
            if (route.isNotBlank()) route
            else if (transaction.note.isNotBlank()) transaction.note
            else formatDateShort(transaction.timestamp)
        }
        transaction.note.isNotBlank() -> transaction.note
        else -> formatDateShort(transaction.timestamp)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.label,
                    tint = category.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.label, fontWeight = FontWeight.Medium)
                    if (transaction.photoPath.isNotBlank()) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = "有小票照片",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(15.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when {
                    isTransfer -> formatMoney(transaction.amount)
                    else -> (if (isIncome) "+" else "-") + formatMoney(transaction.amount)
                },
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = when {
                    isIncome -> IncomeGreen
                    isTransfer -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> ExpenseRed
                }
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
