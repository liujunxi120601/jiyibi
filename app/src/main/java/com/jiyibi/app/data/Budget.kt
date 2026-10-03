package com.jiyibi.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 月度分类预算。
 * @param month 月份，格式 "yyyy-MM"
 * @param categoryKey 分类 key（仅支出分类）
 * @param amount 预算金额
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,
    val categoryKey: String,
    val amount: Double
)
