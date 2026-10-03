package com.jiyibi.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jiyibi.app.MainActivity
import com.jiyibi.app.R
import com.jiyibi.app.util.ReminderPrefs

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!ReminderPrefs.isEnabled(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "jiyibi_reminder"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "记账提醒", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("今天记账了吗？")
            .setContentText("花 10 秒钟记录今天的收支吧 💰")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(2001, notification)
    }
}

/** 开机后重新安排提醒 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && ReminderPrefs.isEnabled(context)) {
            ReminderScheduler.schedule(context)
        }
    }
}
