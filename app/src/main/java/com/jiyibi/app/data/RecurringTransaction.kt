package com.jiyibi.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 周期性账目（如话费、会员订阅）。
 * @param intervalDays 周期天数：1=每天，7=每周，30=每月
 * @param nextDue 下一次应记账时间（毫秒时间戳）
 * @param active 是否启用
 */
@Entity(tableName = "recurring")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val categoryKey: String,
    val note: String = "",
    val intervalDays: Int = 30,
    val nextDue: Long,
    val active: Boolean = true
)
