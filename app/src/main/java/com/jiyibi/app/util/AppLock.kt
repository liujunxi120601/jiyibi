package com.jiyibi.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 应用锁：PIN 码 SHA-256 加盐存储在 SharedPreferences。
 */
object AppLock {
    private const val PREFS = "jiyibi_lock_prefs"
    private const val KEY_HASH = "pin_hash"
    private const val KEY_SALT = "pin_salt"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getString(KEY_HASH, null) != null

    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = sha256(salt + pin.toByteArray())
        prefs(context).edit {
            putString(KEY_HASH, hash.joinToString("") { "%02x".format(it) })
            putString(KEY_SALT, salt.joinToString("") { "%02x".format(it) })
        }
    }

    fun verify(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val hashHex = p.getString(KEY_HASH, null) ?: return false
        val saltHex = p.getString(KEY_SALT, null) ?: return false
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val hash = sha256(salt + pin.toByteArray()).joinToString("") { "%02x".format(it) }
        return hash == hashHex
    }

    fun clear(context: Context) {
        prefs(context).edit { remove(KEY_HASH); remove(KEY_SALT) }
    }

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)
}

/** 记账提醒开关 */
object ReminderPrefs {
    private const val PREFS = "jiyibi_reminder_prefs"
    private const val KEY_ENABLED = "reminder_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ENABLED, enabled)
        }
    }
}
