package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RepeatRule
import com.example.data.Subtask
import com.example.data.TaskCategory
import com.example.data.TaskItem
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    task: TaskItem?,
    defaultDate: String,
    categories: List<TaskCategory>,
    existingSubtasks: List<Subtask> = emptyList(),
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onSave: (TaskItem) -> Unit,
    onSaveWithSubtasks: (TaskItem, List<Subtask>) -> Unit = { t, _ -> onSave(t) },
    onDelete: (TaskItem) -> Unit,
    onAddCategory: (String, String) -> Unit
) {
    if (task == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var title by remember(task) { mutableStateOf(task.title) }
    var note by remember(task) { mutableStateOf(task.note) }
    var category by remember(task) { mutableStateOf(task.category) }
    var date by remember(task) { mutableStateOf<String?>(task.date ?: defaultDate) }
    var startTime by remember(task) { mutableStateOf(task.startTime) }
    var endTime by remember(task) { mutableStateOf(task.endTime) }
    var reminderMinutes by remember(task) { mutableStateOf(task.reminderMinutesBefore) }
    var repeatRule by remember(task) { mutableStateOf(task.repeatRule ?: RepeatRule.NONE) }
    var isPinned by remember(task) { mutableStateOf(task.isPinned) }
    var forwardIfIncomplete by remember(task) { mutableStateOf(task.forwardIfIncomplete) }

    // Subtasks editing state
    var subtasks by remember(task, existingSubtasks) {
        mutableStateOf(existingSubtasks.map { it.copy() })
    }
    var newSubtaskTitle by remember { mutableStateOf("") }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = com.example.ui.theme.ExpressiveShapeTokens.shapeBottomSheet,
        containerColor = colorScheme.surface,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Title & Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickAddDetail(tint = colorScheme.primary)
                    Text(
                        text = if (task.id == 0L) "New Task" else "Edit Task",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                }

                if (task.id != 0L) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete task",
                            tint = colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task Title (Required)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                placeholder = { Text("e.g. Design review meeting") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_editor_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note (Optional plain text)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("Add helpful details...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_editor_note_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SUBTASKS SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUBTASKS (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Existing Subtasks List
            subtasks.forEachIndexed { index, subtask ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                subtasks = subtasks.toMutableList().also { list ->
                                    list[index] = subtask.copy(isCompleted = !subtask.isCompleted)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (subtask.isCompleted) colorScheme.primary else Color.Transparent)
                                .border(
                                    width = 1.5.dp,
                                    color = if (subtask.isCompleted) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (subtask.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = subtask.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (subtask.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                        ),
                        color = if (subtask.isCompleted) colorScheme.onSurface.copy(alpha = 0.5f) else colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            subtasks = subtasks.toMutableList().also { it.removeAt(index) }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove subtask",
                            tint = colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Add new subtask input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskTitle,
                    onValueChange = { newSubtaskTitle = it },
                    placeholder = { Text("Add a subtask...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newSubtaskTitle.isNotBlank()) {
                            subtasks = subtasks + Subtask(
                                taskId = task.id,
                                title = newSubtaskTitle.trim(),
                                isCompleted = false,
                                orderIndex = subtasks.size
                            )
                            newSubtaskTitle = ""
                        }
                    },
                    enabled = newSubtaskTitle.isNotBlank(),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (newSubtaskTitle.isNotBlank()) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add subtask",
                        tint = if (newSubtaskTitle.isNotBlank()) colorScheme.onPrimaryContainer else colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories / Tags
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "None" chip
                QuickChip(
                    label = "None",
                    isSelected = category.isEmpty(),
                    onClick = { category = "" }
                )

                categories.forEach { cat ->
                    QuickChip(
                        label = cat.name,
                        isSelected = category == cat.name,
                        onClick = { category = cat.name }
                    )
                }

                // Add Category chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clickable { showNewCategoryDialog = true }
                        .border(
                            width = 1.dp,
                            color = colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add category",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selection
            Text(
                text = "DATE & TIME",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date picker button
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        val d = date?.let { DateUtils.parseDate(it) }
                        if (d != null) cal.time = d
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val chosenCal = Calendar.getInstance().apply {
                                    set(year, month, dayOfMonth)
                                }
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                date = sdf.format(chosenCal.time)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = date?.let { DateUtils.formatDisplayDate(it) } ?: "Anytime",
                        maxLines = 1
                    )
                }

                if (date != null) {
                    TextButton(onClick = {
                        date = null
                        startTime = null
                        endTime = null
                        reminderMinutes = null
                    }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Start Time & End Time
            if (date != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    startTime = String.format("%02d:%02d", hourOfDay, minute)
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                use24Hour
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = startTime?.let { DateUtils.formatTime(it, use24Hour) } ?: "Start Time",
                            maxLines = 1
                        )
                    }

                    if (startTime != null) {
                        TextButton(onClick = {
                            startTime = null
                            endTime = null
                            reminderMinutes = null
                        }) {
                            Text("No Time", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder selector (Requirement 17)
            Text(
                text = "REMINDER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (date == null || startTime == null) {
                Text(
                    text = "Reminders require a scheduled date and start time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.copy(alpha = 0.45f)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(
                        Pair("None", null),
                        Pair("At time", 0),
                        Pair("5m before", 5),
                        Pair("15m before", 15),
                        Pair("30m before", 30),
                        Pair("1h before", 60)
                    )

                    presets.forEach { (label, minutes) ->
                        QuickChip(
                            label = label,
                            isSelected = reminderMinutes == minutes,
                            onClick = { reminderMinutes = minutes }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Repeat rule selector (Requirement 18)
            Text(
                text = "REPEAT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val rules = listOf(
                    Pair("None", RepeatRule.NONE),
                    Pair("Daily", RepeatRule.DAILY),
                    Pair("Weekdays", RepeatRule.WEEKDAYS),
                    Pair("Weekly", RepeatRule.WEEKLY),
                    Pair("Monthly", RepeatRule.MONTHLY)
                )

                rules.forEach { (label, rule) ->
                    QuickChip(
                        label = label,
                        isSelected = repeatRule == rule,
                        onClick = { repeatRule = rule }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pinned Notification toggle (Requirement 15)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = if (isPinned) Color(0xFFF59E0B) else colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pinned Notification",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Keeps a persistent notification with quick actions visible until done",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(start = 26.dp)
                    )
                }

                Switch(
                    checked = isPinned,
                    onCheckedChange = { isPinned = it }
                )
            }

            // Forward if incomplete toggle (Requirement 13)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Forward if incomplete",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Automatically carries over this task to today's overdue section if unfinished",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }

                Switch(
                    checked = forwardIfIncomplete,
                    onCheckedChange = { forwardIfIncomplete = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Save
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val updatedTask = task.copy(
                            title = title.trim(),
                            note = note.trim(),
                            category = category,
                            date = date,
                            startTime = startTime,
                            endTime = endTime,
                            reminderMinutesBefore = reminderMinutes,
                            repeatRule = if (repeatRule == RepeatRule.NONE) null else repeatRule,
                            isPinned = isPinned,
                            forwardIfIncomplete = forwardIfIncomplete
                        )
                        onSaveWithSubtasks(updatedTask, subtasks)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("task_editor_save_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (task.id == 0L) "Create Task" else "Save Changes",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Dialog: Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete \"${task.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(task)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: New Category Tag
    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("New Tag") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    placeholder = { Text("Tag name (e.g. Finances, Book)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName.trim(), "#6366F1")
                            category = newCategoryName.trim()
                            newCategoryName = ""
                            showNewCategoryDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
