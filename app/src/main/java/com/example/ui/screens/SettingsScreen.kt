package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.QuickChip
import com.example.ui.components.TopCornerBranch
import com.example.ui.components.WidgetAccent
import com.example.ui.theme.SageLight
import com.example.ui.theme.Terracotta
import com.example.ui.theme.Moss
import com.example.ui.theme.Evergreen
import com.example.ui.theme.SageHint

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentThemeName by viewModel.prefs.themeName.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.prefs.themeMode.collectAsStateWithLifecycle()
    val use24Hour by viewModel.prefs.use24HourFormat.collectAsStateWithLifecycle()
    val defaultForward by viewModel.prefs.defaultForwardIncomplete.collectAsStateWithLifecycle()
    val defaultReminder by viewModel.prefs.defaultReminderMinutes.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonString by remember { mutableStateOf("") }

    // Notification permission launcher for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            com.example.notifications.NotificationHelper.createNotificationChannels(context)
            com.example.notifications.NotificationHelper.restoreAllNotifications(context)
            Toast.makeText(context, "Notifications enabled & active tasks restored!", Toast.LENGTH_SHORT).show()
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxSize()) {
        TopCornerBranch(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
        )

        TopCornerBranch(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp),
            mirrored = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onBackground
            )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. APPEARANCE SECTION - EARTHY SAGE THEME
        SettingsSectionHeader(title = "APPEARANCE", icon = Icons.Default.Palette)

        // Single Earthy Sage Theme with Mode selection
        Text(
            text = "Earthy Sage Theme",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Theme preview card showing the natural sage aesthetic
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SageHint.copy(alpha = 0.4f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Moss.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Theme color swatches
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SageLight)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Moss)
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Terracotta)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Natural • Organic • Calm",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Evergreen
                        )
                        Text(
                            text = "Inspired by sage, moss & natural materials",
                            style = MaterialTheme.typography.bodySmall,
                            color = Moss.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode (System, Light, Dark)
        Text(
            text = "Color Mode",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                QuickChip(
                    label = mode.lowercase().replaceFirstChar { it.uppercase() },
                    isSelected = currentThemeMode == mode,
                    onClick = { viewModel.prefs.setThemeMode(mode) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. TIMELINE & TASKS SETTINGS
        SettingsSectionHeader(title = "TIMELINE & SCHEDULING", icon = Icons.Default.Schedule)

        // 24-Hour Format Toggle
        SettingsToggleRow(
            title = "24-hour time format",
            subtitle = "Display times as 14:00 instead of 2:00 PM",
            checked = use24Hour,
            onCheckedChange = { viewModel.prefs.setUse24HourFormat(it) }
        )

        // Default Forward Incomplete Tasks Toggle
        SettingsToggleRow(
            title = "Default forward incomplete",
            subtitle = "Carries over unfinished past tasks into today's timeline",
            checked = defaultForward,
            onCheckedChange = { viewModel.prefs.setDefaultForwardIncomplete(it) }
        )

        // Default Reminder Setting
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Default Reminder",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    Pair("None", -1),
                    Pair("At time", 0),
                    Pair("5m before", 5),
                    Pair("15m before", 15),
                    Pair("30m before", 30)
                )
                presets.forEach { (label, minutes) ->
                    QuickChip(
                        label = label,
                        isSelected = defaultReminder == minutes,
                        onClick = { viewModel.prefs.setDefaultReminderMinutes(minutes) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. NOTIFICATIONS SECTION
        SettingsSectionHeader(title = "NOTIFICATIONS", icon = Icons.Default.Notifications)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Permission",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (hasNotificationPermission) "Enabled • Active alarms & pinned tasks" else "Disabled • Tap to enable notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasNotificationPermission) colorScheme.primary else colorScheme.error
                        )
                    }

                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text("Grant", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pinned tasks create ongoing notifications that remain accessible right on your lock screen with fast \"Mark Done\" and \"Snooze\" buttons.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. DATA / BACKUP SECTION
        SettingsSectionHeader(title = "DATA & BACKUP", icon = Icons.Default.FileDownload)

        OutlinedButton(
            onClick = {
                val json = buildString {
                    append("[\n")
                    allTasks.forEachIndexed { i, t ->
                        append("  {\n")
                        append("    \"id\": ${t.id},\n")
                        append("    \"title\": \"${t.title.replace("\"", "\\\"")}\",\n")
                        append("    \"date\": ${t.date?.let { "\"$it\"" } ?: "null"},\n")
                        append("    \"startTime\": ${t.startTime?.let { "\"$it\"" } ?: "null"},\n")
                        append("    \"category\": \"${t.category}\",\n")
                        append("    \"isCompleted\": ${t.isCompleted}\n")
                        append("  }${if (i < allTasks.lastIndex) "," else ""}\n")
                    }
                    append("]")
                }
                exportJsonString = json
                showExportDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Tasks JSON (${allTasks.size} tasks)")
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Task Export (${allTasks.size} Tasks)") },
            text = {
                Column {
                    Text(
                        text = "Your tasks exported in JSON format:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exportJsonString.take(400) + if (exportJsonString.length > 400) "\n..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Timeline Tasks", exportJsonString))
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        WidgetAccent(
            size = 18.dp,
            alpha = 0.6f,
            modifier = Modifier.padding(end = 6.dp)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
