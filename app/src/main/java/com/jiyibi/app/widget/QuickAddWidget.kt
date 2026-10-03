package com.jiyibi.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import android.content.BroadcastReceiver
import com.jiyibi.app.MainActivity
import com.jiyibi.app.R
import com.jiyibi.app.data.AppDatabase
import com.jiyibi.app.data.Transaction
import com.jiyibi.app.data.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 桌面快速记账小部件。
 * 4 个常用支出一键记账 + 打开 App 按钮。
 */
class QuickAddWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context))
        }
    }

    companion object {
        fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add)

            fun quickAdd(amount: Double, categoryKey: String, label: String, requestCode: Int) {
                val intent = Intent(context, QuickAddReceiver::class.java).apply {
                    action = QuickAddReceiver.ACTION_QUICK_ADD
                    putExtra(QuickAddReceiver.EXTRA_AMOUNT, amount)
                    putExtra(QuickAddReceiver.EXTRA_CATEGORY, categoryKey)
                    putExtra(QuickAddReceiver.EXTRA_LABEL, label)
                }
                val pi = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val viewId = when (requestCode) {
                    1 -> R.id.btn_breakfast
                    2 -> R.id.btn_lunch
                    3 -> R.id.btn_transport
                    else -> R.id.btn_drink
                }
                views.setOnClickPendingIntent(viewId, pi)
            }

            quickAdd(15.0, "food", "早餐", 1)
            quickAdd(30.0, "food", "午餐", 2)
            quickAdd(10.0, "transport", "交通", 3)
            quickAdd(15.0, "food", "奶茶", 4)

            val openIntent = Intent(context, MainActivity::class.java)
            val openPi = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_open_app, openPi)
            return views
        }
    }
}

/** 处理小部件一键记账的广播 */
class QuickAddReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_QUICK_ADD = "com.jiyibi.app.action.QUICK_ADD"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_LABEL = "label"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_QUICK_ADD) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
                val categoryKey = intent.getStringExtra(EXTRA_CATEGORY) ?: "other_expense"
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "支出"
                if (amount > 0) {
                    AppDatabase.get(context).transactionDao().insert(
                        Transaction(
                            amount = amount,
                            type = TransactionType.EXPENSE.name,
                            categoryKey = categoryKey,
                            timestamp = System.currentTimeMillis(),
                            note = "小部件快速记账"
                        )
                    )
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "已记：$label ¥$amount", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
