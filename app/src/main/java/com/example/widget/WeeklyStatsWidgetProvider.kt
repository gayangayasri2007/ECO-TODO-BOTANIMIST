package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeeklyStatsWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetUpdater.ACTION_MIDNIGHT_WIDGET_REFRESH) {
            WidgetUpdater.updateAllWidgets(context)
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Immediate synchronous update to ensure Launcher receives RemoteViews instantly
        for (appWidgetId in appWidgetIds) {
            try {
                val initialViews = RemoteViews(context.packageName, R.layout.widget_weekly_stats)
                appWidgetManager.updateAppWidget(appWidgetId, initialViews)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val today = DateUtils.todayIso()
                val weekDays = DateUtils.getWeekDays(today)
                val allTasks = db.taskDao().getAllTasks()

                // Calculate Monday - Sunday stats
                val dayCounts = weekDays.map { dayIso ->
                    val forDay = allTasks.filter { it.date == dayIso }
                    val completed = forDay.count { it.isCompleted }
                    val total = forDay.size
                    val isToday = dayIso == today
                    Triple(dayIso, completed, total to isToday)
                }

                val totalCompletedThisWeek = dayCounts.sumOf { it.second }
                val totalScheduledThisWeek = dayCounts.sumOf { it.third.first }
                val completionRate = if (totalScheduledThisWeek > 0) {
                    (totalCompletedThisWeek * 100) / totalScheduledThisWeek
                } else if (totalCompletedThisWeek > 0) {
                    100
                } else {
                    0
                }

                // Weekly streak: consecutive completed days up to today
                var streak = 0
                for ((dayIso, completed, pair) in dayCounts) {
                    val total = pair.first
                    if (completed > 0) {
                        streak++
                    } else if (dayIso <= today && total > 0) {
                        streak = 0
                    }
                }

                val dayValIds = listOf(
                    R.id.widget_day_val_mon, R.id.widget_day_val_tue, R.id.widget_day_val_wed,
                    R.id.widget_day_val_thu, R.id.widget_day_val_fri, R.id.widget_day_val_sat,
                    R.id.widget_day_val_sun
                )
                val dayLblIds = listOf(
                    R.id.widget_day_lbl_mon, R.id.widget_day_lbl_tue, R.id.widget_day_lbl_wed,
                    R.id.widget_day_lbl_thu, R.id.widget_day_lbl_fri, R.id.widget_day_lbl_sat,
                    R.id.widget_day_lbl_sun
                )
                val dayBarIds = listOf(
                    R.id.widget_day_bar_mon, R.id.widget_day_bar_tue, R.id.widget_day_bar_wed,
                    R.id.widget_day_bar_thu, R.id.widget_day_bar_fri, R.id.widget_day_bar_sat,
                    R.id.widget_day_bar_sun
                )

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_weekly_stats)

                    // Summary values
                    views.setTextViewText(R.id.widget_stats_completed_count, "$totalCompletedThisWeek")
                    views.setTextViewText(R.id.widget_stats_rate, "$completionRate% done")
                    views.setTextViewText(R.id.widget_stats_streak, "🔥 $streak d")

                    // Click intent -> Open Stats Screen
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("NAVIGATE_TO", "STATS")
                    }
                    val appPendingIntent = PendingIntent.getActivity(
                        context,
                        2001,
                        appIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_stats_root, appPendingIntent)

                    if (totalScheduledThisWeek == 0 && totalCompletedThisWeek == 0) {
                        views.setViewVisibility(R.id.widget_stats_empty, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_stats_content, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_stats_empty, View.GONE)
                        views.setViewVisibility(R.id.widget_stats_content, View.VISIBLE)

                        // 7-day breakdown
                        for (i in 0 until 7) {
                            if (i < dayCounts.size) {
                                val (_, completed, pair) = dayCounts[i]
                                val isToday = pair.second

                                val valViewId = dayValIds[i]
                                val lblViewId = dayLblIds[i]
                                val barViewId = dayBarIds[i]

                                views.setTextViewText(
                                    valViewId,
                                    if (completed > 0) "$completed" else ""
                                )

                                if (isToday) {
                                    views.setTextColor(lblViewId, Color.parseColor("#4D6649")) // Forest Green
                                    views.setTextColor(valViewId, Color.parseColor("#4D6649"))
                                } else {
                                    views.setTextColor(lblViewId, Color.parseColor("#5F7061")) // Muted Sage
                                    views.setTextColor(valViewId, Color.parseColor("#738A6E")) // Moss
                                }

                                if (completed > 0) {
                                    views.setImageViewResource(barViewId, R.drawable.bg_widget_bar_active)
                                } else {
                                    views.setImageViewResource(barViewId, R.drawable.bg_widget_bar)
                                }
                            }
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
