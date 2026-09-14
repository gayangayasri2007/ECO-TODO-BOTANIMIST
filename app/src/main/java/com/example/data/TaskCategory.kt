package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class TaskCategory(
    @PrimaryKey
    val name: String,
    val colorHex: String = "#6366F1",
    val isCustom: Boolean = false
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            TaskCategory("Work", "#3B82F6"),
            TaskCategory("Personal", "#10B981"),
            TaskCategory("Focus", "#8B5CF6"),
            TaskCategory("Errands", "#F59E0B"),
            TaskCategory("Health", "#EC4899")
        )
    }
}
