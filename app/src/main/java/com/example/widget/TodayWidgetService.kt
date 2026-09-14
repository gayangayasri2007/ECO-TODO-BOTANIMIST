package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.AppDatabase
import com.example.data.RepeatRule
import com.example.data.Subtask
import com.example.data.TaskItem
import com.example.util.DateUtils

class TodayWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayWidgetFactory(applicationContext)
    }
}

class TodayWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val items = mutableListOf<FlatDisplayItem>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items.clear()
        try {
            kotlinx.coroutines.runBlocking {
                val db = AppDatabase.getInstance(context)
                val today = DateUtils.todayIso()

                // 1. Overdue forwarded incomplete tasks
                val overdueTasks = db.taskDao().getForwardableIncompleteTasksBefore(today)

                // 2. Today's tasks (timed & untimed)
                val todayTasks = db.taskDao().getTasksForDate(today)

                // 3. Anytime / unscheduled tasks
                val anytimeTasks = db.taskDao().getAnytimeTasks()

                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                val expandedSet = prefs.getStringSet("expanded_tasks", emptySet()) ?: emptySet()

                // Build helper lambda to insert task + subtasks
                val addTaskWithSubtasks: suspend (TaskItem, Boolean, Boolean) -> Unit = { task, isOverdue, isAnytime ->
                    // Synchronously query subtasks for this task
                    val subtaskList = db.taskDao().getSubtasksForTask(task.id)
                    val hasSubtasks = subtaskList.isNotEmpty()
                    val isExpanded = expandedSet.contains(task.id.toString())

                    val completedCount = subtaskList.count { it.isCompleted }
                    val totalCount = subtaskList.size

                    items.add(
                        FlatDisplayItem(
                            isSubtask = false,
                            taskId = task.id,
                            task = task,
                            isOverdue = isOverdue,
                            isAnytime = isAnytime,
                            isExpanded = isExpanded,
                            hasSubtasks = hasSubtasks,
                            completedSubtaskCount = completedCount,
                            totalSubtaskCount = totalCount
                        )
                    )

                    if (hasSubtasks && isExpanded) {
                        subtaskList.forEach { sub ->
                            items.add(
                                FlatDisplayItem(
                                    isSubtask = true,
                                    taskId = task.id,
                                    subtaskId = sub.id,
                                    subtask = sub,
                                    parentCompleted = task.isCompleted
                                )
                            )
                        }
                    }
                }

                // Flatten all lists
                overdueTasks.forEach { addTaskWithSubtasks(it, true, false) }

                val (timed, untimed) = todayTasks.partition { it.startTime != null }
                timed.sortedBy { it.startTime }.forEach { addTaskWithSubtasks(it, false, false) }
                untimed.forEach { addTaskWithSubtasks(it, false, false) }

                anytimeTasks.forEach { addTaskWithSubtasks(it, false, true) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= items.size) return null
        val item = items[position]

        val views = RemoteViews(context.packageName, R.layout.widget_today_item)

        if (!item.isSubtask) {
            // Render Task Item
            views.setViewVisibility(R.id.widget_item_task_container, View.VISIBLE)
            views.setViewVisibility(R.id.widget_item_subtask_container, View.GONE)

            val task = item.task ?: return null

            // Checkbox state & container background
            if (task.isCompleted) {
                views.setTextViewText(R.id.widget_item_check, "☑")
                views.setTextColor(R.id.widget_item_check, Color.parseColor("#738A6E")) // Moss
                views.setInt(R.id.widget_item_task_container, "setBackgroundResource", R.drawable.bg_widget_item_banner_completed)
            } else {
                views.setTextViewText(R.id.widget_item_check, "☐")
                views.setTextColor(R.id.widget_item_check, Color.parseColor("#4D6649")) // Forest Green
                views.setInt(R.id.widget_item_task_container, "setBackgroundResource", R.drawable.bg_widget_item_banner)
            }

            // Checkbox Click fillInIntent
            val checkIntent = Intent().apply {
                putExtra(WidgetUpdater.EXTRA_TASK_ID, task.id)
                putExtra("ACTION_TYPE", "TOGGLE_TASK")
            }
            views.setOnClickFillInIntent(R.id.widget_item_check, checkIntent)

            // Timeline / Time text
            val timeLabel = when {
                item.isOverdue -> "OVERDUE"
                task.startTime != null -> task.startTime
                item.isAnytime -> "ANYTIME"
                else -> ""
            }
            views.setTextViewText(R.id.widget_item_time, timeLabel)
            views.setTextColor(
                R.id.widget_item_time,
                if (item.isOverdue) Color.parseColor("#B37B64") else Color.parseColor("#5F7061")
            )

            // Title & category/pinned badges
            views.setTextViewText(R.id.widget_item_title, task.title)
            views.setTextColor(
                R.id.widget_item_title,
                if (task.isCompleted) Color.parseColor("#869688") else Color.parseColor("#243328")
            )

            val badges = buildString {
                if (task.isPinned) append("📌 ")
                if (task.repeatRule != RepeatRule.NONE && task.repeatRule != null) append("↻ ")
                if (task.reminderMinutesBefore != null) append("🔔 ")
                if (task.category.isNotEmpty()) append(task.category)
                if (item.hasSubtasks) {
                    append(" [${item.completedSubtaskCount}/${item.totalSubtaskCount}]")
                }
            }.trim()

            views.setTextViewText(R.id.widget_item_badge, badges)
            views.setTextColor(R.id.widget_item_badge, Color.parseColor("#5F7061"))

            // Body Click fillInIntent
            val bodyIntent = Intent().apply {
                putExtra(WidgetUpdater.EXTRA_TASK_ID, task.id)
                putExtra("ACTION_TYPE", "VIEW_TASK")
            }
            views.setOnClickFillInIntent(R.id.widget_item_body, bodyIntent)

            // Expand/Collapse Chevron
            if (item.hasSubtasks) {
                views.setViewVisibility(R.id.widget_item_chevron, View.VISIBLE)
                views.setTextViewText(R.id.widget_item_chevron, if (item.isExpanded) "▲" else "▼")

                val chevronIntent = Intent().apply {
                    putExtra(WidgetUpdater.EXTRA_TASK_ID, task.id)
                    putExtra("ACTION_TYPE", "TOGGLE_EXPAND")
                }
                views.setOnClickFillInIntent(R.id.widget_item_chevron, chevronIntent)
            } else {
                views.setViewVisibility(R.id.widget_item_chevron, View.INVISIBLE)
            }

        } else {
            // Render Subtask Item
            views.setViewVisibility(R.id.widget_item_task_container, View.GONE)
            views.setViewVisibility(R.id.widget_item_subtask_container, View.VISIBLE)

            val subtask = item.subtask ?: return null
            val isSubtaskVisuallyCompleted = item.parentCompleted || subtask.isCompleted

            // Subtask Checkbox state
            if (isSubtaskVisuallyCompleted) {
                views.setTextViewText(R.id.widget_item_subtask_check, "☑")
                views.setTextColor(R.id.widget_item_subtask_check, Color.parseColor("#738A6E")) // Moss
            } else {
                views.setTextViewText(R.id.widget_item_subtask_check, "☐")
                views.setTextColor(R.id.widget_item_subtask_check, Color.parseColor("#4D6649")) // Forest Green
            }

            // Subtask Checkbox Click fillInIntent (disabled/mapped to VIEW_TASK if parent is completed)
            val subtaskCheckIntent = Intent().apply {
                if (item.parentCompleted) {
                    putExtra(WidgetUpdater.EXTRA_TASK_ID, item.taskId)
                    putExtra("ACTION_TYPE", "VIEW_TASK")
                } else {
                    putExtra(WidgetUpdater.EXTRA_TASK_ID, item.taskId)
                    putExtra("EXTRA_SUBTASK_ID", subtask.id)
                    putExtra("ACTION_TYPE", "TOGGLE_SUBTASK")
                }
            }
            views.setOnClickFillInIntent(R.id.widget_item_subtask_check, subtaskCheckIntent)

            // Subtask Title
            views.setTextViewText(R.id.widget_item_subtask_title, subtask.title)
            views.setTextColor(
                R.id.widget_item_subtask_title,
                if (isSubtaskVisuallyCompleted) Color.parseColor("#869688") else Color.parseColor("#344C3D")
            )

            // Body Click fillInIntent
            val subtaskBodyIntent = Intent().apply {
                putExtra(WidgetUpdater.EXTRA_TASK_ID, item.taskId)
                putExtra("ACTION_TYPE", "VIEW_TASK")
            }
            views.setOnClickFillInIntent(R.id.widget_item_subtask_title, subtaskBodyIntent)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= items.size) return 0L
        val item = items[position]
        return if (item.isSubtask) {
            item.subtaskId + 1000000L
        } else {
            item.taskId
        }
    }

    override fun hasStableIds(): Boolean = true

    private data class FlatDisplayItem(
        val isSubtask: Boolean,
        val taskId: Long,
        val subtaskId: Long = 0L,
        val task: TaskItem? = null,
        val subtask: Subtask? = null,
        val isOverdue: Boolean = false,
        val isAnytime: Boolean = false,
        val isExpanded: Boolean = false,
        val hasSubtasks: Boolean = false,
        val completedSubtaskCount: Int = 0,
        val totalSubtaskCount: Int = 0,
        val parentCompleted: Boolean = false
    )
}
