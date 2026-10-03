package com.jiyibi.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自定义分类。
 * @param key 唯一 key（"custom_" + 时间戳）
 * @param iconName 图标名称，对应 [categoryIconOptions] 的 key
 * @param colorArgb 颜色 ARGB 整型
 * @param type "EXPENSE" 或 "INCOME"
 */
@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey val key: String,
    val label: String,
    val iconName: String,
    val colorArgb: Int,
    val type: String
)
