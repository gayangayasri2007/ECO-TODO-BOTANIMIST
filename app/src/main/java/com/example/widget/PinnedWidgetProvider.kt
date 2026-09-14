package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PinnedWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Immediate synchronous update so Launcher gets RemoteViews instantly
        for (appWidgetId in appWidgetIds) {
            try {
                val initialViews = RemoteViews(context.packageName, R.layout.widget_pinned)
                appWidgetManager.updateAppWidget(appWidgetId, initialViews)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val pinnedTasks = db.taskDao().getActivePinnedTasks()

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_pinned)
                    views.setTextViewText(R.id.widget_pinned_count, "${pinnedTasks.size} active")

                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val appPendingIntent = PendingIntent.getActivity(
                        context,
                        201,
                        appIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_pinned_root, appPendingIntent)

                    if (pinnedTasks.isEmpty()) {
                        views.setTextViewText(R.id.widget_pinned_item_1, "No pinned tasks. Pin a task to keep it visible!")
                        views.setInt(R.id.widget_pinned_item_1, "setBackgroundResource", R.drawable.bg_widget_item_banner_completed)
                        views.setViewVisibility(R.id.widget_pinned_item_1, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_pinned_item_2, View.GONE)
                        views.setViewVisibility(R.id.widget_pinned_item_3, View.GONE)
                    } else {
                        views.setInt(R.id.widget_pinned_item_1, "setBackgroundResource", R.drawable.bg_widget_item_banner)
                        val t1 = pinnedTasks.getOrNull(0)
                        if (t1 != null) {
                            views.setTextViewText(R.id.widget_pinned_item_1, "📌 ${t1.title}")
                            views.setViewVisibility(R.id.widget_pinned_item_1, View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.widget_pinned_item_1, View.GONE)
                        }

                        val t2 = pinnedTasks.getOrNull(1)
                        if (t2 != null) {
                            views.setTextViewText(R.id.widget_pinned_item_2, "📌 ${t2.title}")
                            views.setViewVisibility(R.id.widget_pinned_item_2, View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.widget_pinned_item_2, View.GONE)
                        }

                        val t3 = pinnedTasks.getOrNull(2)
                        if (t3 != null) {
                            views.setTextViewText(R.id.widget_pinned_item_3, "📌 ${t3.title}")
                            views.setViewVisibility(R.id.widget_pinned_item_3, View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.widget_pinned_item_3, View.GONE)
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
