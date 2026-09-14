package com.example.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.RepeatRule
import com.example.data.Subtask
import com.example.data.TaskItem
import com.example.ui.theme.getThemeColorScheme
import com.example.util.DateUtils
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Centralized Notification and Alarm Engine.
 * 
 * Provides robust, idempotent synchronization of Pinned (ongoing/non-dismissible)
 * notifications and Scheduled alarms directly backed by Room database.
 */
object NotificationEngine {

    const val CHANNEL_PINNED = "channel_pinned_tasks"
    const val CHANNEL_REMINDERS = "channel_task_reminders"

    const val ACTION_MARK_DONE = "com.example.ACTION_MARK_DONE"
    const val ACTION_UNPIN = "com.example.ACTION_UNPIN"
    const val ACTION_TOGGLE_SUBTASK = "com.example.ACTION_TOGGLE_SUBTASK"
    const val ACTION_PREVENT_PINNED_DISMISS = "com.example.ACTION_PREVENT_PINNED_DISMISS"
    const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
    const val ACTION_RESCHEDULE = "com.example.ACTION_RESCHEDULE"
    const val ACTION_REMINDER_ALARM = "com.example.ACTION_REMINDER_ALARM"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_SUBTASK_ID = "extra_subtask_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    const val EXTRA_NEW_DATE = "extra_new_date"

    // Deterministic Notification ID Generators
    fun getPinnedNotificationId(taskId: Long): Int = (taskId * 100 + 1).toInt()
    fun getReminderAlertNotificationId(taskId: Long): Int = (taskId * 100 + 2).toInt()

    // Deterministic Request Codes for PendingIntents
    fun getAlarmRequestCode(taskId: Long): Int = (taskId * 100 + 10).toInt()
    fun getMarkDoneRequestCode(taskId: Long): Int = (taskId * 100 + 11).toInt()
    fun getSubtaskRequestCode(subtaskId: Long): Int = (subtaskId * 100 + 15).toInt()
    fun getDeleteRequestCode(taskId: Long): Int = (taskId * 100 + 16).toInt()
    fun getUnpinRequestCode(taskId: Long): Int = (taskId * 100 + 17).toInt()
    fun getSnoozeRequestCode(taskId: Long): Int = (taskId * 100 + 12).toInt()
    fun getOpenRequestCode(taskId: Long): Int = (taskId * 100 + 13).toInt()
    fun getRescheduleRequestCode(taskId: Long): Int = (taskId * 100 + 14).toInt()

    fun getThemePrimaryColorInt(context: Context): Int {
        return try {
            val prefs = PreferencesManager.getInstance(context)
            val themeName = prefs.themeName.value
            val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val (colorScheme, _) = getThemeColorScheme(themeName, isDark)
            colorScheme.primary.toArgb()
        } catch (e: Exception) {
            0xFF38BDF8.toInt()
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Pinned channel: Persistent, ongoing visibility in shade without sound
            val pinnedChannel = NotificationChannel(
                CHANNEL_PINNED,
                "Pinned Tasks (Ongoing)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Persistent ongoing visibility for active pinned tasks"
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Reminder channel: High importance heads-up alerts with sound and vibration
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely reminders for scheduled tasks"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(pinnedChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    /**
     * Synchronizes all notifications and alarms for a task.
     * Idempotent: Calling multiple times will result in the correct single state.
     */
    fun syncTask(context: Context, task: TaskItem) {
        CoroutineScope(Dispatchers.IO).launch {
            if (task.isCompleted) {
                cancelAllForTask(context, task.id)
                return@launch
            }

            val db = AppDatabase.getInstance(context)
            val subtasks = try {
                db.taskDao().getSubtasksForTask(task.id)
            } catch (e: Exception) {
                emptyList()
            }

            // 1. Sync Pinned Notification
            if (task.isPinned) {
                showPinnedNotification(context, task, subtasks)
            } else {
                cancelPinnedNotification(context, task.id)
            }

            // 2. Sync Reminder Alarm
            scheduleOrCancelReminder(context, task)
        }
    }

    /**
     * Cancels all notifications and alarms associated with a task ID.
     */
    fun cancelAllForTask(context: Context, taskId: Long) {
        cancelPinnedNotification(context, taskId)
        cancelReminderAlertNotification(context, taskId)
        cancelReminderAlarm(context, taskId)
    }

    /**
     * Builds the persistent, non-dismissible pinned Notification instance.
     */
    fun buildPinnedNotification(
        context: Context,
        task: TaskItem,
        subtasks: List<Subtask> = emptyList()
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            getOpenRequestCode(task.id),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Done
        val markDoneIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            getMarkDoneRequestCode(task.id),
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeSub = buildString {
            if (!task.startTime.isNullOrEmpty()) {
                append("at ")
                append(task.startTime)
            }
            if (!task.category.isNullOrEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(task.category)
            }
        }

        val summaryContentText = when {
            subtasks.isNotEmpty() -> {
                val done = subtasks.count { it.isCompleted }
                "$done/${subtasks.size} subtasks completed${if (timeSub.isNotEmpty()) " • $timeSub" else ""}"
            }
            timeSub.isNotEmpty() -> timeSub
            else -> task.note.ifBlank { "Pinned task" }
        }

        // Delete intent ensures that if swipe gesture is attempted on Android 14+, the receiver re-asserts ongoing notification
        val preventDismissIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = ACTION_PREVENT_PINNED_DISMISS
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val preventDismissPendingIntent = PendingIntent.getBroadcast(
            context,
            getDeleteRequestCode(task.id),
            preventDismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Unpin
        val unpinIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = ACTION_UNPIN
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val unpinPendingIntent = PendingIntent.getBroadcast(
            context,
            getUnpinRequestCode(task.id),
            unpinIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // RemoteViews for Collapsed Layout with Inline Checkbox
        val collapsedViews = android.widget.RemoteViews(context.packageName, R.layout.notification_pinned_collapsed).apply {
            setTextViewText(R.id.notif_task_title, task.title)
            setImageViewResource(R.id.notif_task_checkbox, R.drawable.ic_notif_checkbox_unchecked)
            setOnClickPendingIntent(R.id.notif_task_checkbox, markDonePendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_unpin, unpinPendingIntent)
            setOnClickPendingIntent(R.id.notif_task_body, openAppPendingIntent)

            if (subtasks.isNotEmpty()) {
                val completed = subtasks.count { it.isCompleted }
                setTextViewText(R.id.notif_task_progress, "$completed/${subtasks.size}")
                setViewVisibility(R.id.notif_task_progress, android.view.View.VISIBLE)
                setTextViewText(R.id.notif_task_subtitle, if (timeSub.isNotEmpty()) timeSub else task.note.ifBlank { "Tap expand to view checklist" })
            } else {
                setViewVisibility(R.id.notif_task_progress, android.view.View.GONE)
                setTextViewText(R.id.notif_task_subtitle, if (timeSub.isNotEmpty()) timeSub else task.note.ifBlank { "Pinned task" })
            }
        }

        // RemoteViews for Expanded Layout with Interactive Subtask Checklists
        val expandedViews = android.widget.RemoteViews(context.packageName, R.layout.notification_pinned_expanded).apply {
            setTextViewText(R.id.notif_task_title, task.title)
            setImageViewResource(R.id.notif_task_checkbox, R.drawable.ic_notif_checkbox_unchecked)
            setOnClickPendingIntent(R.id.notif_task_checkbox, markDonePendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_unpin, unpinPendingIntent)
            setOnClickPendingIntent(R.id.notif_task_body, openAppPendingIntent)

            if (subtasks.isNotEmpty()) {
                val completed = subtasks.count { it.isCompleted }
                setTextViewText(R.id.notif_task_progress, "$completed/${subtasks.size}")
                setViewVisibility(R.id.notif_task_progress, android.view.View.VISIBLE)
                setTextViewText(R.id.notif_task_subtitle, if (timeSub.isNotEmpty()) timeSub else task.note.ifBlank { "Checklist" })
                setViewVisibility(R.id.notif_divider, android.view.View.VISIBLE)
                removeAllViews(R.id.notif_subtasks_container)

                for (sub in subtasks) {
                    val subView = android.widget.RemoteViews(context.packageName, R.layout.notification_subtask_item)
                    subView.setTextViewText(R.id.notif_subtask_title, sub.title)
                    val checkboxRes = if (sub.isCompleted) {
                        R.drawable.ic_notif_checkbox_checked
                    } else {
                        R.drawable.ic_notif_checkbox_unchecked
                    }
                    subView.setImageViewResource(R.id.notif_subtask_checkbox, checkboxRes)

                    val subtaskIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
                        action = ACTION_TOGGLE_SUBTASK
                        putExtra(EXTRA_SUBTASK_ID, sub.id)
                        putExtra(EXTRA_TASK_ID, task.id)
                    }
                    val subtaskPendingIntent = PendingIntent.getBroadcast(
                        context,
                        getSubtaskRequestCode(sub.id),
                        subtaskIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    subView.setOnClickPendingIntent(R.id.notif_subtask_item_root, subtaskPendingIntent)
                    subView.setOnClickPendingIntent(R.id.notif_subtask_checkbox, subtaskPendingIntent)
                    addView(R.id.notif_subtasks_container, subView)
                }
            } else {
                setViewVisibility(R.id.notif_task_progress, android.view.View.GONE)
                setTextViewText(
                    R.id.notif_task_subtitle,
                    if (timeSub.isNotEmpty()) "$timeSub${if (task.note.isNotBlank()) " • ${task.note}" else ""}" else task.note.ifBlank { "Pinned task" }
                )
                setViewVisibility(R.id.notif_divider, android.view.View.GONE)
                removeAllViews(R.id.notif_subtasks_container)
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_PINNED)
            .setSmallIcon(R.drawable.ic_stat_task)
            .setColor(getThemePrimaryColorInt(context))
            .setColorized(true)
            .setContentTitle("📌 ${task.title}")
            .setContentText(summaryContentText)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSubText("Pinned Task")
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setDeleteIntent(preventDismissPendingIntent)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notification = builder.build()
        // Explicitly enforce ongoing and no-clear flags at notification level
        notification.flags = (notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR) and Notification.FLAG_AUTO_CANCEL.inv()
        return notification
    }

    /**
     * Displays persistent pinned notification with quick actions.
     */
    fun showPinnedNotification(context: Context, task: TaskItem, subtasks: List<Subtask> = emptyList()) {
        if (task.isCompleted || !task.isPinned) {
            cancelPinnedNotification(context, task.id)
            return
        }

        try {
            val notification = buildPinnedNotification(context, task, subtasks)
            NotificationManagerCompat.from(context).notify(getPinnedNotificationId(task.id), notification)
        } catch (e: Exception) {
            android.util.Log.e("NotificationEngine", "Failed to display pinned notification for task ${task.id}", e)
        }
    }

    fun cancelPinnedNotification(context: Context, taskId: Long) {
        try {
            NotificationManagerCompat.from(context).cancel(getPinnedNotificationId(taskId))
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun cancelReminderAlertNotification(context: Context, taskId: Long) {
        try {
            NotificationManagerCompat.from(context).cancel(getReminderAlertNotificationId(taskId))
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Computes the trigger timestamp for a task reminder.
     * Returns null if the task does not have sufficient date/time/reminder information.
     */
    fun calculateTriggerTimestamp(task: TaskItem): Long? {
        val reminderMinutes = task.reminderMinutesBefore ?: return null
        val taskDate = task.date ?: return null
        val startTime = task.startTime ?: return null

        return try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val scheduledDate = dateTimeFormat.parse("$taskDate $startTime") ?: return null

            val cal = Calendar.getInstance()
            cal.time = scheduledDate
            cal.add(Calendar.MINUTE, -reminderMinutes)
            cal.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Schedules or cancels the AlarmManager reminder based on task state.
     */
    fun scheduleOrCancelReminder(context: Context, task: TaskItem) {
        if (task.isCompleted) {
            cancelReminderAlarm(context, task.id)
            return
        }

        val triggerTime = calculateTriggerTimestamp(task)
        if (triggerTime == null || triggerTime <= System.currentTimeMillis()) {
            // Not a future timed reminder -> cancel any previously scheduled alarm
            cancelReminderAlarm(context, task.id)
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
                action = ACTION_REMINDER_ALARM
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_TITLE, task.title)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getAlarmRequestCode(task.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationEngine", "Failed to schedule reminder for task ${task.id}", e)
        }
    }

    fun cancelReminderAlarm(context: Context, taskId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
                action = ACTION_REMINDER_ALARM
                putExtra(EXTRA_TASK_ID, taskId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getAlarmRequestCode(taskId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Displays a high-priority heads-up reminder notification when an alarm fires.
     */
    fun onReminderAlarmFired(context: Context, taskId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val task = db.taskDao().getTaskById(taskId)
            val subtasks = try {
                db.taskDao().getSubtasksForTask(taskId)
            } catch (e: Exception) {
                emptyList()
            }
            if (task != null && !task.isCompleted) {
                showReminderAlertNotification(context, task, subtasks)
            }
        }
    }

    /**
     * Builds standard regular dismissible reminder notification.
     */
    fun buildReminderAlertNotification(
        context: Context,
        task: TaskItem,
        subtasks: List<Subtask> = emptyList()
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            (getOpenRequestCode(task.id) + 500),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            (getMarkDoneRequestCode(task.id) + 500),
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_SNOOZE_MINUTES, 15)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (getSnoozeRequestCode(task.id) + 500),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = buildString {
            if (!task.startTime.isNullOrEmpty()) {
                append("Scheduled at ")
                append(task.startTime)
            } else {
                append("Scheduled task due now")
            }
            if (task.note.isNotBlank()) {
                append(" • ")
                append(task.note)
            }
        }

        val subtaskListText = if (subtasks.isNotEmpty()) {
            subtasks.joinToString("\n") { sub ->
                if (sub.isCompleted) "  ☑ ${sub.title}" else "  ☐ ${sub.title}"
            }
        } else ""

        val bigTextContent = buildString {
            append(subtitle)
            if (subtaskListText.isNotEmpty()) {
                append("\n\nSubtasks:\n")
                append(subtaskListText)
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_task)
            .setColor(getThemePrimaryColorInt(context))
            .setColorized(true)
            .setContentTitle("⏰ Reminder: ${task.title}")
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openPendingIntent)

        builder.addAction(android.R.drawable.checkbox_on_background, "☐ Complete", markDonePendingIntent)

        val firstSub = subtasks.firstOrNull { !it.isCompleted } ?: subtasks.firstOrNull()
        if (firstSub != null) {
            val subtaskIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
                action = ACTION_TOGGLE_SUBTASK
                putExtra(EXTRA_SUBTASK_ID, firstSub.id)
            }
            val subtaskPendingIntent = PendingIntent.getBroadcast(
                context,
                (getSubtaskRequestCode(firstSub.id) + 500),
                subtaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val symbol = if (firstSub.isCompleted) "☑" else "☐"
            builder.addAction(android.R.drawable.checkbox_on_background, "$symbol ${firstSub.title}", subtaskPendingIntent)
        }

        builder.addAction(android.R.drawable.ic_popup_sync, "Snooze 15m", snoozePendingIntent)

        return builder.build()
    }

    private fun showReminderAlertNotification(context: Context, task: TaskItem, subtasks: List<Subtask> = emptyList()) {
        try {
            val notification = buildReminderAlertNotification(context, task, subtasks)
            NotificationManagerCompat.from(context).notify(getReminderAlertNotificationId(task.id), notification)
        } catch (e: Exception) {
            android.util.Log.e("NotificationEngine", "Failed to display reminder alert for task ${task.id}", e)
        }
    }

    /**
     * Executes true database task completion from a notification action.
     */
    suspend fun handleMarkDone(context: Context, taskId: Long) {
        val db = AppDatabase.getInstance(context)
        val dao = db.taskDao()
        val task = dao.getTaskById(taskId) ?: return

        if (!task.isCompleted) {
            dao.updateTaskCompletion(taskId, true, System.currentTimeMillis())
            cancelAllForTask(context, taskId)

            // Handle recurring task occurrence
            if (!task.repeatRule.isNullOrEmpty() && task.repeatRule != RepeatRule.NONE && task.date != null) {
                val nextDate = computeNextRecurringDate(task.date, task.repeatRule)
                val nextTask = task.copy(
                    id = 0L,
                    date = nextDate,
                    isCompleted = false,
                    completedAt = null,
                    createdAt = System.currentTimeMillis()
                )
                val nextId = dao.insertTask(nextTask)

                // Copy subtasks for new recurring instance with completed = false
                val existingSubtasks = dao.getSubtasksForTask(taskId)
                if (existingSubtasks.isNotEmpty()) {
                    val newSubtasks = existingSubtasks.map {
                        it.copy(id = 0L, taskId = nextId, isCompleted = false, completedAt = null)
                    }
                    dao.insertSubtasks(newSubtasks)
                }

                val fullNextTask = nextTask.copy(id = nextId)
                syncTask(context, fullNextTask)
            }

            WidgetUpdater.updateAllWidgets(context)
        }
    }

    /**
     * Handles unpinning a task from notification action.
     */
    suspend fun handleUnpin(context: Context, taskId: Long) {
        val db = AppDatabase.getInstance(context)
        val dao = db.taskDao()
        val task = dao.getTaskById(taskId) ?: return

        if (task.isPinned) {
            dao.updateTaskPinned(taskId, false)
            cancelPinnedNotification(context, taskId)
            WidgetUpdater.updateAllWidgets(context)
        }
    }

    /**
     * Toggles a subtask completion from notification action and synchronizes parent.
     */
    suspend fun handleToggleSubtask(context: Context, subtaskId: Long, taskId: Long) {
        val db = AppDatabase.getInstance(context)
        val dao = db.taskDao()
        val subtask = dao.getSubtaskById(subtaskId) ?: return
        val newCompleted = !subtask.isCompleted
        val completedAt = if (newCompleted) System.currentTimeMillis() else null
        dao.updateSubtask(subtask.copy(isCompleted = newCompleted, completedAt = completedAt))
        val parentTask = dao.getTaskById(subtask.taskId)
        if (parentTask != null) {
            syncTask(context, parentTask)
        }
        try {
            WidgetUpdater.updateAllWidgets(context)
        } catch (_: Throwable) {
        }
    }

    /**
     * Handles snooze from notification by setting a temporary future alarm.
     */
    fun handleSnooze(context: Context, taskId: Long, snoozeMinutes: Int = 15) {
        cancelReminderAlertNotification(context, taskId)

        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
                action = ACTION_REMINDER_ALARM
                putExtra(EXTRA_TASK_ID, taskId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getAlarmRequestCode(taskId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Restores all eligible notifications and reminders after device reboot or app launch.
     */
    fun restoreAllNotifications(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val dao = db.taskDao()
                val allTasks = dao.getAllTasks()

                for (task in allTasks) {
                    if (task.isCompleted) {
                        cancelAllForTask(context, task.id)
                    } else {
                        val subtasks = try {
                            dao.getSubtasksForTask(task.id)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        if (task.isPinned) {
                            showPinnedNotification(context, task, subtasks)
                        } else {
                            cancelPinnedNotification(context, task.id)
                        }
                        scheduleOrCancelReminder(context, task)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun computeNextRecurringDate(currentDateIso: String, rule: String): String {
        val cal = Calendar.getInstance()
        val parsed = DateUtils.parseDate(currentDateIso) ?: return DateUtils.offsetDate(currentDateIso, 1)
        cal.time = parsed

        when (rule) {
            RepeatRule.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RepeatRule.WEEKDAYS -> {
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            RepeatRule.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatRule.MONTHLY -> cal.add(Calendar.MONTH, 1)
            else -> cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }
}
