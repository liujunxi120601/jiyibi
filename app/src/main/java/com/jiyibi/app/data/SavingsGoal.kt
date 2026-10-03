package com.jiyibi.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 储蓄目标（存钱罐）。
 * @param deadline 目标日期（毫秒时间戳），0 表示不设期限
 */
@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val deadline: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
