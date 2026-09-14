package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val taskId = intent.getLongExtra(NotificationEngine.EXTRA_TASK_ID, -1L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    NotificationEngine.ACTION_MARK_DONE -> {
                        if (taskId != -1L) {
                            NotificationEngine.handleMarkDone(context, taskId)
                        }
                    }

                    NotificationEngine.ACTION_UNPIN -> {
                        if (taskId != -1L) {
                            NotificationEngine.handleUnpin(context, taskId)
                        }
                    }

                    NotificationEngine.ACTION_PREVENT_PINNED_DISMISS -> {
                        if (taskId != -1L) {
                            val db = com.example.data.AppDatabase.getInstance(context)
                            val task = db.taskDao().getTaskById(taskId)
                            if (task != null && task.isPinned && !task.isCompleted) {
                                val subtasks = db.taskDao().getSubtasksForTask(taskId)
                                NotificationEngine.showPinnedNotification(context, task, subtasks)
                            }
                        }
                    }

                    NotificationEngine.ACTION_TOGGLE_SUBTASK -> {
                        val subtaskId = intent.getLongExtra(NotificationEngine.EXTRA_SUBTASK_ID, -1L)
                        if (subtaskId != -1L) {
                            NotificationEngine.handleToggleSubtask(context, subtaskId, taskId)
                        }
                    }

                    NotificationEngine.ACTION_SNOOZE -> {
                        if (taskId != -1L) {
                            val snoozeMinutes = intent.getIntExtra(NotificationEngine.EXTRA_SNOOZE_MINUTES, 15)
                            NotificationEngine.handleSnooze(context, taskId, snoozeMinutes)
                        }
                    }

                    NotificationEngine.ACTION_RESCHEDULE -> {
                        if (taskId != -1L) {
                            val newDate = intent.getStringExtra(NotificationEngine.EXTRA_NEW_DATE)
                                ?: com.example.util.DateUtils.offsetDate(com.example.util.DateUtils.todayIso(), 1)
                            val db = com.example.data.AppDatabase.getInstance(context)
                            db.taskDao().rescheduleTask(taskId, newDate)
                            val updatedTask = db.taskDao().getTaskById(taskId)
                            if (updatedTask != null) {
                                NotificationEngine.syncTask(context, updatedTask)
                            }
                            com.example.widget.WidgetUpdater.updateAllWidgets(context)
                        }
                    }

                    NotificationEngine.ACTION_REMINDER_ALARM -> {
                        if (taskId != -1L) {
                            NotificationEngine.onReminderAlarmFired(context, taskId)
                        }
                    }

                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        NotificationEngine.restoreAllNotifications(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

