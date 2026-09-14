package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TaskItem
import com.example.ui.MainViewModel
import com.example.ui.components.TopCornerBranch
import com.example.ui.components.BotanicalContrast
import com.example.ui.components.BotanicalDrawables
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val today = DateUtils.todayIso()
    val weekDays = DateUtils.getWeekDays(today)

    // Calculate Monday-Sunday completed count and total for each day
    val dailyStats = weekDays.map { dayIso ->
        val forDay = allTasks.filter { it.date == dayIso }
        val completed = forDay.count { it.isCompleted }
        val total = forDay.size
        val label = DateUtils.getDayOfWeekLabel(dayIso)
        val isCurrentDay = dayIso == today
        DailyStat(dayIso, label, completed, total, isCurrentDay)
    }

    val maxDailyCompleted = dailyStats.maxOfOrNull { it.completed }?.coerceAtLeast(1) ?: 1
    val totalCompletedThisWeek = dailyStats.sumOf { it.completed }
    val totalScheduledThisWeek = dailyStats.sumOf { it.total }
    val completionRate = if (totalScheduledThisWeek > 0) {
        (totalCompletedThisWeek * 100) / totalScheduledThisWeek
    } else if (totalCompletedThisWeek > 0) {
        100
    } else {
        0
    }

    // Weekly streak: consecutive active completed days up to today
    var streak = 0
    for (stat in dailyStats) {
        if (stat.completed > 0) {
            streak++
        } else if (stat.dayIso <= today && stat.total > 0) {
            streak = 0
        }
    }

    // Category breakdown
    val categoryCounts = allTasks
        .filter { it.isCompleted && it.category.isNotEmpty() }
        .groupingBy { it.category }
        .eachCount()

    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxSize()) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Botanical Overview Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                ) {
                    Image(
                        painter = painterResource(id = BotanicalDrawables.HeroCard),
                        contentDescription = "Botanical foliage header",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Weekly Growth",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "$totalCompletedThisWeek completed • $completionRate% consistency",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Weekly Overview",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onBackground
                )
                Text(
                    text = "Monday → Sunday progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

        Spacer(modifier = Modifier.height(20.dp))

        // Key Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Completed Count
            MetricCard(
                icon = Icons.Default.CheckCircle,
                iconTint = colorScheme.primary,
                value = "$totalCompletedThisWeek",
                label = "Completed",
                modifier = Modifier.weight(1f)
            )

            // Completion Rate
            MetricCard(
                icon = Icons.Default.PieChart,
                iconTint = colorScheme.secondary,
                value = "$completionRate%",
                label = "Rate",
                modifier = Modifier.weight(1f)
            )

            // Streak
            MetricCard(
                icon = Icons.Default.LocalFireDepartment,
                iconTint = Color(0xFFF97316),
                value = "$streak d",
                label = "Streak",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Activity Chart (Mon - Sun)
        Surface(
            shape = com.example.ui.theme.ExpressiveShapeTokens.shapeExpressiveCard,
            color = colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colorScheme.outline.copy(alpha = 0.2f),
                    shape = com.example.ui.theme.ExpressiveShapeTokens.shapeExpressiveCard
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DAILY BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bar Chart Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyStats.forEach { stat ->
                        val barHeightFraction = (stat.completed.toFloat() / maxDailyCompleted.toFloat()).coerceIn(0.08f, 1f)
                        val animatedFraction by animateFloatAsState(
                            targetValue = barHeightFraction,
                            animationSpec = tween(500),
                            label = "bar_height_${stat.label}"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (stat.completed > 0) "${stat.completed}" else "",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.primary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Bar
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height((90 * animatedFraction).dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (stat.isToday) colorScheme.primary
                                        else if (stat.completed > 0) colorScheme.primary.copy(alpha = 0.6f)
                                        else colorScheme.outline.copy(alpha = 0.18f)
                                    )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stat.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (stat.isToday) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (stat.isToday) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Categories Distribution
        if (categoryCounts.isNotEmpty()) {
            Text(
                text = "COMPLETED BY CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categoryCounts.forEach { (cat, count) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
}

@Composable
fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        shape = com.example.ui.theme.ExpressiveShapeTokens.shapeExpressiveCard,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.border(
            width = 1.dp,
            color = colorScheme.outline.copy(alpha = 0.2f),
            shape = com.example.ui.theme.ExpressiveShapeTokens.shapeExpressiveCard
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private data class DailyStat(
    val dayIso: String,
    val label: String,
    val completed: Int,
    val total: Int,
    val isToday: Boolean
)
