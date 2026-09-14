package com.example.data

import android.content.Context
import com.example.notifications.NotificationEngine
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val context: Context,
    private val taskDao: TaskDao
) {
    val allTasksFlow: Flow<List<TaskItem>> = taskDao.getAllTasksFlow()
    val allCategoriesFlow: Flow<List<TaskCategory>> = taskDao.getAllCategoriesFlow()
    val activePinnedTasksFlow: Flow<List<TaskItem>> = taskDao.getActivePinnedTasksFlow()

    fun getTasksForDateFlow(date: String): Flow<List<TaskItem>> =
        taskDao.getTasksForDateFlow(date)

    fun getAnytimeTasksFlow(): Flow<List<TaskItem>> =
        taskDao.getAnytimeTasksFlow()

    fun getForwardableIncompleteTasksBeforeFlow(date: String): Flow<List<TaskItem>> =
        taskDao.getForwardableIncompleteTasksBeforeFlow(date)

    suspend fun getTaskById(id: Long): TaskItem? = taskDao.getTaskById(id)

    suspend fun saveTask(task: TaskItem): Long {
        val id = if (task.id == 0L) {
            taskDao.insertTask(task)
        } else {
            taskDao.updateTask(task)
            task.id
        }

        val savedTask = task.copy(id = id)
        NotificationEngine.syncTask(context, savedTask)
        WidgetUpdater.updateAllWidgets(context)
        return id
    }

    suspend fun saveTaskWithSubtasks(task: TaskItem, subtasks: List<Subtask>): Long {
        val taskId = saveTask(task)
        saveSubtasks(taskId, subtasks)
        return taskId
    }

    suspend fun toggleCompletion(task: TaskItem) {
        val newCompletedState = !task.isCompleted
        val completedAt = if (newCompletedState) System.currentTimeMillis() else null

        taskDao.updateTaskCompletion(task.id, newCompletedState, completedAt)

        if (newCompletedState) {
            NotificationEngine.cancelAllForTask(context, task.id)

            // If recurring, generate next occurrence for future without duplicating past instance
            if (!task.repeatRule.isNullOrEmpty() && task.repeatRule != RepeatRule.NONE && task.date != null) {
                val nextDate = NotificationEngine.computeNextRecurringDate(task.date, task.repeatRule)
                val nextTask = task.copy(
                    id = 0L,
                    date = nextDate,
                    isCompleted = false,
                    completedAt = null,
                    createdAt = System.currentTimeMillis()
                )
                val nextId = taskDao.insertTask(nextTask)

                // Copy subtasks for the new occurrence
                val existingSubtasks = taskDao.getSubtasksForTask(task.id)
                if (existingSubtasks.isNotEmpty()) {
                    val newSubtasks = existingSubtasks.map {
                        it.copy(id = 0L, taskId = nextId, isCompleted = false, completedAt = null)
                    }
                    taskDao.insertSubtasks(newSubtasks)
                }

                val fullNextTask = nextTask.copy(id = nextId)
                NotificationEngine.syncTask(context, fullNextTask)
            }
        } else {
            // Uncompleting - restore notification and alarm if still valid in future
            val uncompletedTask = task.copy(isCompleted = false, completedAt = null)
            NotificationEngine.syncTask(context, uncompletedTask)
        }

        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun deleteTask(task: TaskItem) {
        taskDao.deleteTask(task)
        NotificationEngine.cancelAllForTask(context, task.id)
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun togglePin(task: TaskItem) {
        val newPinned = !task.isPinned
        taskDao.updateTaskPinned(task.id, newPinned)
        val updated = task.copy(isPinned = newPinned)
        NotificationEngine.syncTask(context, updated)
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun forwardTaskToDate(task: TaskItem, newDate: String) {
        val orig = task.originalDate ?: task.date
        val updated = task.copy(date = newDate, originalDate = orig)
        taskDao.updateTask(updated)
        NotificationEngine.syncTask(context, updated)
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun updateTaskOrder(tasks: List<TaskItem>) {
        tasks.forEachIndexed { index, item ->
            if (item.orderIndex != index) {
                taskDao.updateTask(item.copy(orderIndex = index))
            }
        }
    }

    suspend fun addCategory(category: TaskCategory) {
        taskDao.insertCategory(category)
    }

    suspend fun deleteCategory(name: String) {
        taskDao.deleteCategory(name)
    }

    // Subtask management
    fun getSubtasksForTaskFlow(taskId: Long): Flow<List<Subtask>> =
        taskDao.getSubtasksForTaskFlow(taskId)

    fun getAllSubtasksFlow(): Flow<List<Subtask>> =
        taskDao.getAllSubtasksFlow()

    suspend fun getSubtasksForTask(taskId: Long): List<Subtask> =
        taskDao.getSubtasksForTask(taskId)

    suspend fun saveSubtasks(taskId: Long, subtasks: List<Subtask>) {
        taskDao.deleteSubtasksByTaskId(taskId)
        if (subtasks.isNotEmpty()) {
            val toInsert = subtasks.mapIndexed { index, subtask ->
                subtask.copy(id = 0L, taskId = taskId, orderIndex = index)
            }
            taskDao.insertSubtasks(toInsert)
        }
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun toggleSubtaskCompletion(subtaskId: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        taskDao.updateSubtaskCompletion(subtaskId, isCompleted, completedAt)
        
        val subtask = taskDao.getSubtaskById(subtaskId)
        if (subtask != null) {
            val task = taskDao.getTaskById(subtask.taskId)
            if (task != null) {
                NotificationEngine.syncTask(context, task)
            }
        }
        
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun addSubtask(taskId: Long, title: String) {
        val count = taskDao.getSubtasksForTask(taskId).size
        taskDao.insertSubtask(Subtask(taskId = taskId, title = title, orderIndex = count))
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun deleteSubtask(subtaskId: Long) {
        taskDao.deleteSubtaskById(subtaskId)
        WidgetUpdater.updateAllWidgets(context)
    }
}

