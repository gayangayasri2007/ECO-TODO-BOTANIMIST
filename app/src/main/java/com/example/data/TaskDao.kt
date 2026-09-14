package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, startTime ASC, createdAt ASC")
    fun getAllTasksFlow(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, startTime ASC, createdAt ASC")
    suspend fun getAllTasks(): List<TaskItem>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY orderIndex ASC, startTime ASC, createdAt ASC")
    fun getTasksForDateFlow(date: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY orderIndex ASC, startTime ASC, createdAt ASC")
    suspend fun getTasksForDate(date: String): List<TaskItem>

    @Query("SELECT * FROM tasks WHERE date IS NULL ORDER BY orderIndex ASC, createdAt ASC")
    fun getAnytimeTasksFlow(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE date IS NULL ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getAnytimeTasks(): List<TaskItem>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND date IS NOT NULL AND date < :date AND forwardIfIncomplete = 1 ORDER BY date ASC, startTime ASC")
    fun getForwardableIncompleteTasksBeforeFlow(date: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND date IS NOT NULL AND date < :date AND forwardIfIncomplete = 1 ORDER BY date ASC, startTime ASC")
    suspend fun getForwardableIncompleteTasksBefore(date: String): List<TaskItem>

    @Query("SELECT * FROM tasks WHERE isPinned = 1 AND isCompleted = 0 ORDER BY date ASC, startTime ASC")
    fun getActivePinnedTasksFlow(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE isPinned = 1 AND isCompleted = 0")
    suspend fun getActivePinnedTasks(): List<TaskItem>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskItem?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Long): Flow<TaskItem?>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1")
    fun getAllCompletedTasksFlow(): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskItem>)

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE tasks SET isPinned = :isPinned WHERE id = :id")
    suspend fun updateTaskPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE tasks SET date = :newDate WHERE id = :id")
    suspend fun rescheduleTask(id: Long, newDate: String?)

    // Category queries
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<TaskCategory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: TaskCategory)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<TaskCategory>)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategory(name: String)

    // Subtask queries
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY orderIndex ASC, id ASC")
    fun getSubtasksForTaskFlow(taskId: Long): Flow<List<Subtask>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY orderIndex ASC, id ASC")
    suspend fun getSubtasksForTask(taskId: Long): List<Subtask>

    @Query("SELECT * FROM subtasks ORDER BY taskId ASC, orderIndex ASC")
    fun getAllSubtasksFlow(): Flow<List<Subtask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: Subtask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<Subtask>)

    @Update
    suspend fun updateSubtask(subtask: Subtask)

    @Delete
    suspend fun deleteSubtask(subtask: Subtask)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubtaskById(id: Long)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: Long)

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    suspend fun getAllIncompleteTasks(): List<TaskItem>

    @Query("SELECT * FROM subtasks WHERE id = :id")
    suspend fun getSubtaskById(id: Long): Subtask?

    @Query("UPDATE subtasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun updateSubtaskCompletion(id: Long, isCompleted: Boolean, completedAt: Long?)
}
