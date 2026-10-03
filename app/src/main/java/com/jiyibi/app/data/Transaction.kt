package com.jiyibi.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一笔记录。
 * @param type 取值为 "EXPENSE"（支出）、"INCOME"（收入）或 "TRANSFER"（转账，不计入收支统计）
 * @param categoryKey 分类 key，对应 [Category.key]
 * @param timestamp 记录时间（毫秒时间戳）
 * @param photoPath 小票照片的本地文件路径（可空）
 * @param transferFrom 转账转出方备注（仅转账类型使用）
 * @param transferTo 转账转入方备注（仅转账类型使用）
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val categoryKey: String,
    val timestamp: Long,
    val note: String = "",
    val photoPath: String = "",
    val transferFrom: String = "",
    val transferTo: String = ""
)
