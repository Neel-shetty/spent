package com.neel.spent

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.neel.spent.utils.SpendingCalculator
import com.neel.spent.utils.SmsReader

/**
 * Implementation of App Widget functionality.
 */
class SpentWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == REFRESH_ACTION) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                // Double tap detected, launch MainActivity
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(launchIntent)
            } else {
                // Single tap, update widget
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(intent.component)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            lastTapTime = currentTime
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {
        const val REFRESH_ACTION = "com.neel.spent.REFRESH_WIDGET"
        private var lastTapTime = 0L
        private const val DOUBLE_TAP_TIMEOUT = 500L // 500ms timeout for double tap
    }
}

//internal fun updateAppWidget(
//    context: Context,
//    appWidgetManager: AppWidgetManager,
//    appWidgetId: Int
//) {
//    val transactions = SmsReader.readTransactions(context)
//    val today = SpendingCalculator.getTodaySpending(transactions)
//    val thisWeek = SpendingCalculator.getWeekSpending(transactions)
//    val thisMonth = SpendingCalculator.getMonthSpending(transactions)
//
//    val views = RemoteViews(context.packageName, R.layout.spent_widget)
//
//    views.setTextViewText(R.id.daily_amount, "D ₹%.0f".format(today))
//    views.setTextViewText(R.id.weekly_amount, "W ₹%.0f".format(thisWeek))
//    views.setTextViewText(R.id.monthly_amount, "M ₹%.0f".format(thisMonth))
//
//    views.setFloat(R.id.daily_amount, "setTextSize", 28f)
//    views.setFloat(R.id.weekly_amount, "setTextSize", 28f)
//    views.setFloat(R.id.monthly_amount, "setTextSize", 28f)
//
//    val refreshIntent = Intent(context, SpentWidget::class.java).apply {
//        action = SpentWidget.REFRESH_ACTION
//    }
//    val refreshPendingIntent = PendingIntent.getBroadcast(
//        context, 0, refreshIntent,
//        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//    )
//    views.setOnClickPendingIntent(R.id.widget_container, refreshPendingIntent)
//
//    appWidgetManager.updateAppWidget(appWidgetId, views)
//}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    // Get the current dimensions of the widget.
    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
    // We use the minimum height in dp, which is available when the widget is resized.
    val heightInDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

    // --- HEURISTIC FOR TEXT SIZE ---
    // This is a simple calculation to determine the text size based on the widget's height.
    // The divisor (e.g., 7f) is a "magic number" you can adjust to make the text
    // larger or smaller. A smaller divisor means larger text.
    // We use coerceIn to set a minimum and maximum size to prevent the text from
    // becoming too small or ridiculously large.
    val textSize = (heightInDp / 6f).coerceIn(12f, 44f)


    val transactions = SmsReader.readTransactions(context)
    val today = SpendingCalculator.getTodaySpending(transactions)
    val thisWeek = SpendingCalculator.getWeekSpending(transactions)
    val thisMonth = SpendingCalculator.getMonthSpending(transactions)

    val views = RemoteViews(context.packageName, R.layout.spent_widget)

    views.setTextViewText(R.id.daily_amount, "D ₹%.0f".format(today))
    views.setTextViewText(R.id.weekly_amount, "W ₹%.0f".format(thisWeek))
    views.setTextViewText(R.id.monthly_amount, "M ₹%.0f".format(thisMonth))

    views.setFloat(R.id.daily_amount, "setTextSize", textSize)
    views.setFloat(R.id.weekly_amount, "setTextSize", textSize)
    views.setFloat(R.id.monthly_amount, "setTextSize", textSize)

    val refreshIntent = Intent(context, SpentWidget::class.java).apply {
        action = SpentWidget.REFRESH_ACTION
        component = android.content.ComponentName(context, SpentWidget::class.java)
    }
    val refreshPendingIntent = PendingIntent.getBroadcast(
        context, 0, refreshIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_container, refreshPendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}