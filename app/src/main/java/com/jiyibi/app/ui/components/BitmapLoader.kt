package com.jiyibi.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 从本地文件加载 Bitmap（带下采样，避免大图 OOM）。
 * @param path 图片绝对路径
 * @param maxSize 最大边长（像素），默认 1024
 */
@Composable
fun rememberBitmapFromFile(path: String, maxSize: Int = 1024): Bitmap? {
    return remember(path) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@remember null
            var sampleSize = 1
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / sampleSize > maxSize) sampleSize *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }
}
