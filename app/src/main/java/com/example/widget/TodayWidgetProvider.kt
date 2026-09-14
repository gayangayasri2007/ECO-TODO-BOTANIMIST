package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.widget.ACTION_ITEM_CLICK") {
            val taskId = intent.getLongExtra(WidgetUpdater.EXTRA_TASK_ID, -1L)
            val subtaskId = intent.getLongExtra("EXTRA_SUBTASK_ID", -1L)
            val actionType = intent.getStringExtra("ACTION_TYPE")

            when (actionType) {
                "TOGGLE_TASK" -> {
                    if (taskId != -1L) {
                        val pendingResult = goAsync()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = AppDatabase.getInstance(context)
                                val repository = TaskRepository(context, db.taskDao())
                                val task = db.taskDao().getTaskById(taskId)
                                if (task != null) {
                                    repository.toggleCompletion(task)
                                    WidgetUpdater.updateAllWidgets(context)
                                }
                            } catch (t: Throwable) {
                                t.printStackTrace()
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }
                }
                "TOGGLE_SUBTASK" -> {
                    if (subtaskId != -1L) {
                        val pendingResult = goAsync()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = AppDatabase.getInstance(context)
                                val parentTask = db.taskDao().getTaskById(taskId)
                                if (parentTask != null && parentTask.isCompleted) {
                                    // Prevent toggling subtasks when parent task is completed
                                    return@launch
                                }
                                val subtaskList = db.taskDao().getSubtasksForTask(taskId)
                                val subtask = subtaskList.find { it.id == subtaskId }
                                if (subtask != null) {
                                    val newCompleted = !subtask.isCompleted
                                    val newCompletedAt = if (newCompleted) System.currentTimeMillis() else null
                                    db.taskDao().updateSubtaskCompletion(subtaskId, newCompleted, newCompletedAt)
                                    WidgetUpdater.updateAllWidgets(context)
                                }
                            } catch (t: Throwable) {
                                t.printStackTrace()
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }
                }
                "TOGGLE_EXPAND" -> {
                    if (taskId != -1L) {
                        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                        val expandedSet = prefs.getStringSet("expanded_tasks", emptySet())?.toMutableSet() ?: mutableSetOf()
                        val taskIdStr = taskId.toString()
                        if (expandedSet.contains(taskIdStr)) {
                            expandedSet.remove(taskIdStr)
                        } else {
                            expandedSet.add(taskIdStr)
                        }
                        prefs.edit().putStringSet("expanded_tasks", expandedSet).apply()

                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val component = ComponentName(context, TodayWidgetProvider::class.java)
                        val widgetIds = appWidgetManager.getAppWidgetIds(component)
                        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_list_view)
                    }
                }
                "VIEW_TASK" -> {
                    if (taskId != -1L) {
                        val appIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("EXTRA_TASK_ID", taskId)
                        }
                        context.startActivity(appIntent)
                    }
                }
            }
        } else if (intent.action == WidgetUpdater.ACTION_MIDNIGHT_WIDGET_REFRESH) {
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
        // Immediate synchronous update to show initial views instantly
        for (appWidgetId in appWidgetIds) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_today)
                // Set Adapter
                val serviceIntent = Intent(context, TodayWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
                views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_container)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val today = DateUtils.todayIso()

                val overdueTasks = db.taskDao().getForwardableIncompleteTasksBefore(today)
                val todayTasks = db.taskDao().getTasksForDate(today)
                val anytimeTasks = db.taskDao().getAnytimeTasks()

                val totalCount = overdueTasks.size + todayTasks.size + anytimeTasks.size
                val completedCount = overdueTasks.count { it.isCompleted } +
                        todayTasks.count { it.isCompleted } +
                        anytimeTasks.count { it.isCompleted }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_today)

                    // Header Info
                    views.setTextViewText(R.id.widget_date_label, DateUtils.formatDisplayDate(today))
                    views.setTextViewText(
                        R.id.widget_progress_badge,
                        if (totalCount > 0 && completedCount == totalCount) "All done!" else "$completedCount / $totalCount"
                    )

                    // Root click -> Open App
                    val rootIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val rootPendingIntent = PendingIntent.getActivity(
                        context,
                        2001,
                        rootIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, rootPendingIntent)

                    // Add click -> Open App with quick add extra
                    val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("ACTION_QUICK_ADD", true)
                    }
                    val quickAddPendingIntent = PendingIntent.getActivity(
                        context,
                        2002,
                        quickAddIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_add, quickAddPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_empty_btn_add, quickAddPendingIntent)

                    // Set remote adapter
                    val serviceIntent = Intent(context, TodayWidgetService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                    }
                    views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
                    views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_container)

                    // Setup click PendingIntent template for items in the ListView
                    val itemClickIntent = Intent(context, TodayWidgetProvider::class.java).apply {
                        action = "com.example.widget.ACTION_ITEM_CLICK"
                    }
                    val itemClickPendingIntent = PendingIntent.getBroadcast(
                        context,
                        2003,
                        itemClickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                    views.setPendingIntentTemplate(R.id.widget_list_view, itemClickPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                }

                WidgetUpdater.scheduleMidnightRollover(context)
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
