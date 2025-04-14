package com.neel.spent

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*
import com.neel.spent.data.Transaction

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

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val transactions = readSMS(context)
    val today = getTodaySpending(transactions)
    val thisWeek = getWeekSpending(transactions)
    val thisMonth = getMonthSpending(transactions)

    // Calculate progress percentages (assuming daily budget of 1000, weekly 5000, monthly 20000)
    val dailyProgress = (today / 1000.0 * 360).coerceIn(0.0, 360.0)  // Convert to degrees
    val weeklyProgress = (thisWeek / 5000.0 * 360).coerceIn(0.0, 360.0)
    val monthlyProgress = (thisMonth / 20000.0 * 360).coerceIn(0.0, 360.0)

    val views = RemoteViews(context.packageName, R.layout.spent_widget)
    
    // Set progress for each arc
    views.setProgressBar(R.id.daily_progress, 360, dailyProgress.toInt(), false)
    views.setProgressBar(R.id.weekly_progress, 360, weeklyProgress.toInt(), false)
    views.setProgressBar(R.id.monthly_progress, 360, monthlyProgress.toInt(), false)
    
    // Set amounts
    views.setTextViewText(R.id.daily_amount, "D ₹%.0f".format(today))
    views.setTextViewText(R.id.weekly_amount, "W ₹%.0f".format(thisWeek))
    views.setTextViewText(R.id.monthly_amount, "M ₹%.0f".format(thisMonth))
    
    // Set text sizes (in sp units)
    views.setFloat(R.id.daily_amount, "setTextSize", 28f)
    views.setFloat(R.id.weekly_amount, "setTextSize", 28f)
    views.setFloat(R.id.monthly_amount, "setTextSize", 28f)
    
    // Add click handler for refresh
    val refreshIntent = Intent(context, SpentWidget::class.java).apply {
        action = SpentWidget.REFRESH_ACTION
    }
    val refreshPendingIntent = PendingIntent.getBroadcast(
        context, 0, refreshIntent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_container, refreshPendingIntent)
    
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun readSMS(context: Context): List<Transaction> {
    val cursor: Cursor? = context.contentResolver.query(
        Uri.parse("content://sms/inbox"),
        arrayOf("body", "address", "date"),
        "body LIKE ?",
        arrayOf("%debited%"),
        "date DESC"
    )

    val transactions = mutableListOf<Transaction>()
    val amountRegex = Regex("Rs\\s+(\\d+\\.?\\d*)\\s+debited")
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    cursor?.use { 
        while (it.moveToNext()) {
            val body = it.getString(0)
            amountRegex.find(body)?.let { match ->
                val amount = match.groupValues[1].toDouble()
                val dateStr = body.substringAfter("on ").substringBefore(" to")
                val date = dateFormat.parse(dateStr) ?: Date()
                transactions.add(Transaction(amount, date))
            }
        }
    }
    return transactions
}

private fun getTodaySpending(transactions: List<Transaction>): Double {
    val cal = Calendar.getInstance()
    return transactions.filter { transaction ->
        val txnCal = Calendar.getInstance().apply { time = transaction.date }
        cal.get(Calendar.YEAR) == txnCal.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == txnCal.get(Calendar.DAY_OF_YEAR)
    }.sumOf { it.amount }
}

private fun getWeekSpending(transactions: List<Transaction>): Double {
    val cal = Calendar.getInstance()
    return transactions.filter { transaction ->
        val txnCal = Calendar.getInstance().apply { time = transaction.date }
        cal.get(Calendar.YEAR) == txnCal.get(Calendar.YEAR) &&
        cal.get(Calendar.WEEK_OF_YEAR) == txnCal.get(Calendar.WEEK_OF_YEAR)
    }.sumOf { it.amount }
}

private fun getMonthSpending(transactions: List<Transaction>): Double {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    val startOfMonth = cal.time

    return transactions.filter { transaction ->
        transaction.date >= startOfMonth
    }.sumOf { it.amount }
}