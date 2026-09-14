package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TaskItem
import com.example.ui.MainViewModel
import com.example.ui.components.BotanicalContrast
import com.example.ui.components.BotanicalDrawables
import com.example.ui.components.CurrentTimeIndicator
import com.example.ui.components.DateNavigator
import com.example.ui.components.EmptyTimelineState
import com.example.ui.components.OverdueSection
import com.example.ui.components.TaskCardContent
import com.example.ui.components.TimelineTaskRow
import com.example.ui.components.TopCornerBranch
import com.example.ui.components.UnscheduledSection
import com.example.ui.theme.ExpressiveShapeTokens
import com.example.ui.theme.LocalTimelineTokens
import com.example.util.DateUtils

@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    use24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val timelineState by viewModel.timelineState.collectAsStateWithLifecycle()
    val allSubtasks by viewModel.allSubtasks.collectAsStateWithLifecycle()
    val lastCompletedTask by viewModel.lastCompletedTask.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val areNotificationsEnabled = remember(timelineState) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val subtasksByTaskId = remember(allSubtasks) {
        allSubtasks.groupBy { it.taskId }
    }

    // Handle Undo Snackbar
    LaunchedEffect(lastCompletedTask) {
        val task = lastCompletedTask
        if (task != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Task completed",
                actionLabel = "UNDO",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoCompletion()
            } else {
                viewModel.clearLastCompleted()
            }
        }
    }

    val totalTaskCount = timelineState.timedTasks.size +
            timelineState.dateOnlyTasks.size +
            timelineState.overdueTasks.size +
            timelineState.unscheduledTasks.size

    val completedCount = timelineState.timedTasks.count { it.isCompleted } +
            timelineState.dateOnlyTasks.count { it.isCompleted } +
            timelineState.unscheduledTasks.count { it.isCompleted }

    Box(modifier = modifier.fillMaxSize()) {
        // Highlighting top corner botanical branch
        TopCornerBranch(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp),
            size = 120.dp,
            alpha = BotanicalContrast.CORNER_BRANCH
        )

        TopCornerBranch(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp),
            size = 120.dp,
            alpha = BotanicalContrast.CORNER_BRANCH,
            mirrored = true
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Date Navigation Header
            DateNavigator(
                selectedDate = timelineState.selectedDate,
                onDateSelected = { viewModel.setSelectedDate(it) },
                onPreviousDay = { viewModel.offsetSelectedDate(-1) },
                onNextDay = { viewModel.offsetSelectedDate(1) },
                onReturnToToday = { viewModel.goToToday() },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
            )

            // Lush Botanical Header Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                ) {
                    Image(
                        painter = painterResource(id = BotanicalDrawables.HeroCard),
                        contentDescription = "Botanical foliage header",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                    // Soft gradient overlay for text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Cultivate Today",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (totalTaskCount == 0) "A peaceful canvas awaits" else "$completedCount of $totalTaskCount completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Image(
                            painter = painterResource(id = BotanicalDrawables.WidgetAccent),
                            contentDescription = null,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Minimal Day Progress Bar or Status subtitle with animated track (Requirement 64 & 68)
            if (totalTaskCount > 0) {
                val progressFraction = completedCount.toFloat() / totalTaskCount.toFloat()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (completedCount == totalTaskCount && totalTaskCount > 0)
                                "All tasks complete today"
                            else
                                "$completedCount of $totalTaskCount completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(ExpressiveShapeTokens.shapePill)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(3.dp)
                                .clip(ExpressiveShapeTokens.shapePill)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Notification Status Alert Banner (if disabled)
            if (!areNotificationsEnabled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications disabled",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Enable to view pinned ongoing checklists and alarms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        TextButton(onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to general application details
                                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(fallback)
                            }
                        }) {
                            Text(
                                "Enable",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Main Timeline Content
            if (totalTaskCount == 0) {
                EmptyTimelineState(
                    onAddTask = { viewModel.openQuickAdd() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("timeline_lazy_column")
                ) {
                    // 1. OVERDUE SECTION (Forwarded incomplete tasks from past)
                    if (timelineState.overdueTasks.isNotEmpty()) {
                        item(key = "section_overdue") {
                            OverdueSection(
                                overdueTasks = timelineState.overdueTasks,
                                use24Hour = use24Hour,
                                subtasksMap = subtasksByTaskId,
                                onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                                onToggleSubtask = { subtaskId, isCompleted ->
                                    viewModel.toggleSubtaskCompletion(subtaskId, isCompleted)
                                },
                                onClick = { viewModel.openTaskEditor(it) },
                                onForwardToToday = { viewModel.forwardOverdueTask(it) }
                            )
                        }
                    }

                    // 1.5 CURRENT TIME INDICATOR on Today (Requirement 66)
                    if (DateUtils.isToday(timelineState.selectedDate)) {
                        item(key = "current_time_beacon") {
                            CurrentTimeIndicator(use24Hour = use24Hour)
                        }
                    }

                    // 2. TIMED TASKS (Listed Chronological Timeline)
                    if (timelineState.timedTasks.isNotEmpty()) {
                        item(key = "header_timeline") {
                            Text(
                                text = "TIMELINE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }

                        itemsIndexed(
                            items = timelineState.timedTasks,
                            key = { _, task -> "timed_${task.id}" }
                        ) { index, task ->
                            TimelineTaskRow(
                                task = task,
                                isFirst = index == 0,
                                isLast = index == timelineState.timedTasks.lastIndex && timelineState.dateOnlyTasks.isEmpty(),
                                use24Hour = use24Hour,
                                subtasks = subtasksByTaskId[task.id] ?: emptyList(),
                                onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                                onToggleSubtask = { subtaskId, isCompleted ->
                                    viewModel.toggleSubtaskCompletion(subtaskId, isCompleted)
                                },
                                onClick = { viewModel.openTaskEditor(it) },
                                onLongClick = { viewModel.openTaskEditor(it) }
                            )
                        }
                    }

                    // 3. DATE-ONLY TASKS (Tasks scheduled today with no specific time)
                    if (timelineState.dateOnlyTasks.isNotEmpty()) {
                        item(key = "header_date_only") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ALL DAY / DATE-ONLY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }

                        itemsIndexed(
                            items = timelineState.dateOnlyTasks,
                            key = { _, task -> "date_only_${task.id}" }
                        ) { _, task ->
                            TaskCardContent(
                                task = task,
                                use24Hour = use24Hour,
                                subtasks = subtasksByTaskId[task.id] ?: emptyList(),
                                onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                                onToggleSubtask = { subtaskId, isCompleted ->
                                    viewModel.toggleSubtaskCompletion(subtaskId, isCompleted)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { viewModel.openTaskEditor(task) }
                            )
                        }
                    }

                    // 4. UNSCHEDULED / ANYTIME TASKS
                    if (timelineState.unscheduledTasks.isNotEmpty()) {
                        item(key = "section_unscheduled") {
                            UnscheduledSection(
                                tasks = timelineState.unscheduledTasks,
                                use24Hour = use24Hour,
                                subtasksMap = subtasksByTaskId,
                                onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                                onToggleSubtask = { subtaskId, isCompleted ->
                                    viewModel.toggleSubtaskCompletion(subtaskId, isCompleted)
                                },
                                onClick = { viewModel.openTaskEditor(it) }
                            )
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }

        // Global + Quick Add FAB with expressive squircle shape (Requirement 62 & 81)
        FloatingActionButton(
            onClick = { viewModel.openQuickAdd() },
            shape = ExpressiveShapeTokens.shapeMedium,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_quick_add")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task",
                modifier = Modifier.size(24.dp)
            )
        }

        // Snackbar Host for Undo
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}
