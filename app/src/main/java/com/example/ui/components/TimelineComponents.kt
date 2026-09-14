package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import com.example.ui.theme.Evergreen
import com.example.ui.theme.Terracotta
import com.example.ui.theme.Moss
import com.example.ui.theme.Sage
import com.example.ui.theme.SageLight
import com.example.ui.theme.SageHint
import com.example.ui.theme.Ivory
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Subtask
import com.example.data.TaskItem
import com.example.ui.theme.ExpressiveMotionTokens
import com.example.ui.theme.ExpressiveShapeTokens
import com.example.ui.theme.LocalTimelineTokens
import com.example.util.DateUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineTaskRow(
    task: TaskItem,
    isFirst: Boolean,
    isLast: Boolean,
    use24Hour: Boolean,
    subtasks: List<Subtask> = emptyList(),
    onToggleCompletion: (TaskItem) -> Unit,
    onClick: (TaskItem) -> Unit,
    onLongClick: (TaskItem) -> Unit = {},
    onToggleSubtask: ((Long, Boolean) -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val timelineTokens = LocalTimelineTokens.current
    val colorScheme = MaterialTheme.colorScheme

    val timeLabel = task.startTime?.let { DateUtils.formatTime(it, use24Hour) } ?: ""

    // Tactile press state
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = ExpressiveMotionTokens.springMedium(),
        label = "task_press_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick(task) },
                onLongClick = { onLongClick(task) }
            )
            .padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left: Time Label with expressive typography
        Box(
            modifier = Modifier
                .width(62.dp)
                .padding(top = 12.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            if (timeLabel.isNotEmpty()) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    ),
                    color = if (task.isCompleted) timelineTokens.nodeCompleted else colorScheme.onBackground.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Center: Continuous Timeline line & node with expressive geometry
        Box(
            modifier = Modifier
                .width(20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Draw continuous line
            Canvas(modifier = Modifier.height(72.dp).width(2.dp)) {
                val startY = if (isFirst) size.height * 0.25f else 0f
                val endY = if (isLast) size.height * 0.25f else size.height
                drawLine(
                    color = timelineTokens.timelineLine,
                    start = Offset(x = size.width / 2, y = startY),
                    end = Offset(x = size.width / 2, y = endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Distinct botanical stem accent along timeline node line
            TimelineEdgeStem(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(18.dp)
                    .height(72.dp)
            )

            // Node dot with smooth transition
            val nodeColor by animateColorAsState(
                targetValue = if (task.isCompleted) timelineTokens.nodeCompleted else timelineTokens.nodeActive,
                animationSpec = tween(ExpressiveMotionTokens.durationMedium),
                label = "node_color"
            )
            val nodeRadius by animateFloatAsState(
                targetValue = if (isSelected) 12f else 10f,
                animationSpec = ExpressiveMotionTokens.springSoft(),
                label = "node_radius"
            )

            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .size(nodeRadius.dp)
                    .clip(CircleShape)
                    .background(nodeColor)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Task Card content
        TaskCardContent(
            task = task,
            subtasks = subtasks,
            use24Hour = use24Hour,
            onToggleCompletion = onToggleCompletion,
            onToggleSubtask = onToggleSubtask,
            isSelected = isSelected,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 3.dp)
        )
    }
}

@Composable
fun TaskCardContent(
    task: TaskItem,
    use24Hour: Boolean,
    onToggleCompletion: (TaskItem) -> Unit,
    subtasks: List<Subtask> = emptyList(),
    onToggleSubtask: ((Long, Boolean) -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }

    // Spring-based completion transition
    val alphaAnim by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.55f else 1f,
        animationSpec = tween(durationMillis = ExpressiveMotionTokens.durationMedium),
        label = "completion_alpha"
    )

    // Contextual Adaptive Shape (Requirement 62 & 81)
    val cardShape = when {
        isSelected -> ExpressiveShapeTokens.shapeSelectedTask
        task.isCompleted -> RoundedCornerShape(10.dp)
        else -> ExpressiveShapeTokens.shapeExpressiveCard
    }

    val cardBgColor by animateColorAsState(
        targetValue = when {
            isSelected -> colorScheme.primaryContainer.copy(alpha = 0.35f)
            task.isCompleted -> colorScheme.surfaceVariant.copy(alpha = 0.25f)
            else -> colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(ExpressiveMotionTokens.durationMedium),
        label = "card_bg_color"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> colorScheme.primary.copy(alpha = 0.6f)
            task.isCompleted -> colorScheme.outline.copy(alpha = 0.15f)
            else -> colorScheme.outline.copy(alpha = 0.28f)
        },
        animationSpec = tween(ExpressiveMotionTokens.durationMedium),
        label = "card_border_color"
    )

    Surface(
        shape = cardShape,
        color = cardBgColor,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = cardShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accessible Checkbox / Completion Control with physical spring response (Requirement 68)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onToggleCompletion(task) }
                        .testTag("task_completion_control_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    val boxBg by animateColorAsState(
                        targetValue = if (task.isCompleted) colorScheme.primary else Color.Transparent,
                        animationSpec = ExpressiveMotionTokens.springMedium(),
                        label = "checkbox_bg"
                    )
                    val checkScale by animateFloatAsState(
                        targetValue = if (task.isCompleted) 1f else 0.4f,
                        animationSpec = ExpressiveMotionTokens.springSoft(),
                        label = "check_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(boxBg)
                            .border(
                                width = 1.5.dp,
                                color = if (task.isCompleted) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(7.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(checkScale)
                            )
                        }
                    }
                }

                // Task Details Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    // Priority 1: Title (Requirement 96)
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = colorScheme.onSurface.copy(alpha = alphaAnim),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle metadata: category chip, subtasks count, repeat, pinned, reminder, note
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category chip (Material 3 Expressive pill)
                        if (task.category.isNotEmpty()) {
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.14f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Subtasks badge with expandable chevron
                        if (subtasks.isNotEmpty()) {
                            val doneCount = subtasks.count { it.isCompleted }
                            val isAllDone = doneCount == subtasks.size
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isAllDone) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable { isExpanded = !isExpanded }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircleOutline,
                                        contentDescription = "Subtasks",
                                        tint = if (isAllDone) colorScheme.primary else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isAllDone && !task.isCompleted) "All subtasks" else "$doneCount/${subtasks.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (isAllDone) colorScheme.primary else colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle subtasks",
                                        tint = if (isAllDone) colorScheme.primary else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // Pinned icon badge (Requirement 85)
                        if (task.isPinned) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Evergreen.copy(alpha = 0.15f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pinned",
                                        tint = Evergreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "PIN",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = Evergreen
                                    )
                                }
                            }
                        }

                        // Repeat indicator (Requirement 87)
                        if (!task.repeatRule.isNullOrEmpty() && task.repeatRule != "NONE") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = "Repeating",
                                    tint = colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "↻",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.secondary
                                )
                            }
                        }

                        // Reminder badge (Requirement 86)
                        if (task.reminderMinutesBefore != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Reminder set",
                                    tint = colorScheme.tertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                if (task.startTime != null) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = DateUtils.formatTime(task.startTime, use24Hour),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = colorScheme.tertiary
                                    )
                                }
                            }
                        }

                        // Note excerpt indicator
                        if (task.note.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "Has note",
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Inline Subtask Checklist Preview (Only rendered when expanded)
            if (subtasks.isNotEmpty() && isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 4.dp, top = 2.dp)
                ) {
                    subtasks.forEach { subtask ->
                        val isSubtaskVisuallyCompleted = task.isCompleted || subtask.isCompleted
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !task.isCompleted) {
                                    onToggleSubtask?.invoke(subtask.id, !subtask.isCompleted)
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSubtaskVisuallyCompleted) colorScheme.primary.copy(alpha = 0.8f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSubtaskVisuallyCompleted) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(4.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSubtaskVisuallyCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed subtask",
                                            tint = colorScheme.onPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = subtask.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    textDecoration = if (isSubtaskVisuallyCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (isSubtaskVisuallyCompleted) colorScheme.onSurface.copy(alpha = 0.45f) else colorScheme.onSurface.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Requirement 66: Current Time Indicator with subtle alive pulse and flowing accent glow
 */
@Composable
fun CurrentTimeIndicator(
    use24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val timelineTokens = LocalTimelineTokens.current
    val colorScheme = MaterialTheme.colorScheme

    val now = remember { java.util.Calendar.getInstance() }
    val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = now.get(java.util.Calendar.MINUTE)
    val timeString = if (use24Hour) {
        String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
    } else {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour % 12 == 0) 12 else hour % 12
        String.format(java.util.Locale.US, "%d:%02d %s", h, minute, amPm)
    }

    // Subtle breathing pulse (alive but calm, non-distracting)
    val infiniteTransition = rememberInfiniteTransition(label = "time_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left time badge
        Box(
            modifier = Modifier.width(62.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = timelineTokens.currentTimeLine.copy(alpha = 0.18f),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = timelineTokens.currentTimeLine.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp)
                )
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = timelineTokens.currentTimeLine,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Center: Glowing beacon
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient outer glow
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(timelineTokens.accentGlow.copy(alpha = pulseAlpha * 0.4f))
            )
            // Core indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(timelineTokens.currentTimeLine)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Flowing highlight line across the timeline
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(timelineTokens.currentTimeLine.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun OverdueSection(
    overdueTasks: List<TaskItem>,
    use24Hour: Boolean,
    subtasksMap: Map<Long, List<Subtask>> = emptyMap(),
    onToggleCompletion: (TaskItem) -> Unit,
    onToggleSubtask: ((Long, Boolean) -> Unit)? = null,
    onClick: (TaskItem) -> Unit,
    onForwardToToday: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (overdueTasks.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Terracotta)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "OVERDUE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = Terracotta
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "• Forwarded from previous dates",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        overdueTasks.forEach { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TaskCardContent(
                    task = task,
                    subtasks = subtasksMap[task.id] ?: emptyList(),
                    use24Hour = use24Hour,
                    onToggleCompletion = onToggleCompletion,
                    onToggleSubtask = onToggleSubtask,
                    modifier = Modifier.weight(1f).clickable { onClick(task) }
                )
                // Forward button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onForwardToToday(task) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Forward,
                        contentDescription = "Forward to today",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UnscheduledSection(
    tasks: List<TaskItem>,
    use24Hour: Boolean,
    subtasksMap: Map<Long, List<Subtask>> = emptyMap(),
    onToggleCompletion: (TaskItem) -> Unit,
    onToggleSubtask: ((Long, Boolean) -> Unit)? = null,
    onClick: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "UNSCHEDULED / ANYTIME",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )
        }

        tasks.forEach { task ->
            TaskCardContent(
                task = task,
                subtasks = subtasksMap[task.id] ?: emptyList(),
                use24Hour = use24Hour,
                onToggleCompletion = onToggleCompletion,
                onToggleSubtask = onToggleSubtask,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onClick(task) }
            )
        }
    }
}

@Composable
fun EmptyTimelineState(
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    BotanicalEmptyState(
        title = "Your day is clear.",
        subtitle = "Capture anything instantly, or enjoy the calm space.",
        modifier = modifier,
        action = {
            Surface(
                shape = ExpressiveShapeTokens.shapePill,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier
                    .clickable { onAddTask() }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = ExpressiveShapeTokens.shapePill
                    )
            ) {
                Text(
                    text = "+ Add your first task",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    )
}

