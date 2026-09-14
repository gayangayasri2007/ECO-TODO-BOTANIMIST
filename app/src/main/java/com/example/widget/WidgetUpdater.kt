package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.util.Calendar

object WidgetUpdater {

    const val ACTION_WIDGET_TOGGLE_TASK = "com.example.widget.ACTION_TOGGLE_TASK"
    const val ACTION_MIDNIGHT_WIDGET_REFRESH = "com.example.widget.ACTION_MIDNIGHT_REFRESH"
    const val EXTRA_TASK_ID = "EXTRA_TASK_ID"

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // 1. Today Widget
        val todayComponent = ComponentName(context, TodayWidgetProvider::class.java)
        val todayIds = appWidgetManager.getAppWidgetIds(todayComponent)
        if (todayIds != null && todayIds.isNotEmpty()) {
            val intent = Intent(context, TodayWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, todayIds)
            }
            context.sendBroadcast(intent)
        }

        // 2. Weekly Stats Widget
        val statsComponent = ComponentName(context, WeeklyStatsWidgetProvider::class.java)
        val statsIds = appWidgetManager.getAppWidgetIds(statsComponent)
        if (statsIds != null && statsIds.isNotEmpty()) {
            val intent = Intent(context, WeeklyStatsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, statsIds)
            }
            context.sendBroadcast(intent)
        }

        // 3. Pinned Widget
        val pinnedComponent = ComponentName(context, PinnedWidgetProvider::class.java)
        val pinnedIds = appWidgetManager.getAppWidgetIds(pinnedComponent)
        if (pinnedIds != null && pinnedIds.isNotEmpty()) {
            val intent = Intent(context, PinnedWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, pinnedIds)
            }
            context.sendBroadcast(intent)
        }

        // Ensure midnight rollover alarm is active
        scheduleMidnightRollover(context)
    }

    fun scheduleMidnightRollover(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, TodayWidgetProvider::class.java).apply {
            action = ACTION_MIDNIGHT_WIDGET_REFRESH
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9901,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Graceful fallback for exact alarm permission constraints
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
