package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val category: String = "",
    val date: String? = null,           // "YYYY-MM-DD" or null for Anytime tasks
    val startTime: String? = null,      // "HH:mm" or null for Date-only tasks
    val endTime: String? = null,        // "HH:mm" or null
    val reminderMinutesBefore: Int? = null, // null, 0 (at time), 5, 15, 30, 60
    val repeatRule: String? = null,     // null, "DAILY", "WEEKDAYS", "WEEKLY", "MONTHLY"
    val forwardIfIncomplete: Boolean = true,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val originalDate: String? = null    // Traceable original date when forwarded
) {
    val isAnytime: Boolean
        get() = date == null

    val isDateOnly: Boolean
        get() = date != null && startTime == null

    val isTimed: Boolean
        get() = date != null && startTime != null
}

object RepeatRule {
    const val NONE = "NONE"
    const val DAILY = "DAILY"
    const val WEEKDAYS = "WEEKDAYS"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
}
