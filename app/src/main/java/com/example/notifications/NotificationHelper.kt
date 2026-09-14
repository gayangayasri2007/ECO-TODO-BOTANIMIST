package com.example.notifications

import android.content.Context
import com.example.data.TaskItem

object NotificationHelper {
    const val CHANNEL_PINNED = NotificationEngine.CHANNEL_PINNED
    const val CHANNEL_REMINDERS = NotificationEngine.CHANNEL_REMINDERS

    const val ACTION_MARK_DONE = NotificationEngine.ACTION_MARK_DONE
    const val ACTION_SNOOZE = NotificationEngine.ACTION_SNOOZE
    const val ACTION_RESCHEDULE = NotificationEngine.ACTION_RESCHEDULE
    const val ACTION_REMINDER_ALARM = NotificationEngine.ACTION_REMINDER_ALARM

    const val EXTRA_TASK_ID = NotificationEngine.EXTRA_TASK_ID
    const val EXTRA_TASK_TITLE = NotificationEngine.EXTRA_TASK_TITLE

    fun createNotificationChannels(context: Context) {
        NotificationEngine.createNotificationChannels(context)
    }

    fun showPinnedNotification(context: Context, task: TaskItem) {
        NotificationEngine.showPinnedNotification(context, task)
    }

    fun cancelPinnedNotification(context: Context, taskId: Long) {
        NotificationEngine.cancelPinnedNotification(context, taskId)
    }

    fun scheduleReminder(context: Context, task: TaskItem) {
        NotificationEngine.scheduleOrCancelReminder(context, task)
    }

    fun cancelReminder(context: Context, taskId: Long) {
        NotificationEngine.cancelReminderAlarm(context, taskId)
    }

    fun cancelAllForTask(context: Context, taskId: Long) {
        NotificationEngine.cancelAllForTask(context, taskId)
    }

    fun restoreAllNotifications(context: Context) {
        NotificationEngine.restoreAllNotifications(context)
    }
}

