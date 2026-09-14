package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.util.DateUtils
import java.util.Calendar

@Composable
fun DateNavigator(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onReturnToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isToday = DateUtils.isToday(selectedDate)
    val displayDate = DateUtils.formatFullDate(selectedDate)

    // Swipe gesture accumulator
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        totalDrag += delta
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (totalDrag > 60f) {
                        // Swiped right -> go to previous day
                        onPreviousDay()
                    } else if (totalDrag < -60f) {
                        // Swiped left -> go to next day
                        onNextDay()
                    }
                    totalDrag = 0f
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Day Button
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier
                .size(48.dp)
                .testTag("prev_day_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous day",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Center: Date Display + Date Picker trigger
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(com.example.ui.theme.ExpressiveShapeTokens.shapeSmall)
                .clickable {
                    val cal = Calendar.getInstance()
                    val d = DateUtils.parseDate(selectedDate)
                    if (d != null) cal.time = d
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val chosenCal = Calendar.getInstance().apply {
                                set(year, month, dayOfMonth)
                            }
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            onDateSelected(sdf.format(chosenCal.time))
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("date_selector_button")
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Choose Date",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // "Today" pill button if not currently viewing today (Requirement 72)
            AnimatedVisibility(
                visible = !isToday,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = com.example.ui.theme.ExpressiveShapeTokens.shapePill,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clickable { onReturnToToday() }
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            shape = com.example.ui.theme.ExpressiveShapeTokens.shapePill
                        )
                        .testTag("return_to_today_button")
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Next Day Button
            IconButton(
                onClick = onNextDay,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("next_day_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next day",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
