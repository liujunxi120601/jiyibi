package com.jiyibi.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一笔收支记录。
 * @param type 取值为 "EXPENSE"（支出）或 "INCOME"（收入）
 * @param categoryKey 分类 key，对应 [Category.key]
 * @param timestamp 记录时间（毫秒时间戳）
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val categoryKey: String,
    val timestamp: Long,
    val note: String = ""
)
