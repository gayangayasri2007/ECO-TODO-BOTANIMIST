package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.ui.components.BotanicalBackgroundContainer
import com.example.notifications.NotificationHelper
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.QuickAddSheet
import com.example.ui.components.TaskEditorSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TodayScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.example.ui.theme.ExpressiveMotionTokens
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Notification channels and restore active pinned notifications
        NotificationHelper.createNotificationChannels(this)
        NotificationHelper.restoreAllNotifications(this)

        handleIntent(intent)

        setContent {
            val context = this
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    NotificationHelper.restoreAllNotifications(context)
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val themeMode by viewModel.prefs.themeMode.collectAsStateWithLifecycle()
            val use24Hour by viewModel.prefs.use24HourFormat.collectAsStateWithLifecycle()

            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            val isQuickAddOpen by viewModel.isQuickAddOpen.collectAsStateWithLifecycle()
            val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
            val editingTaskSubtasks by viewModel.editingTaskSubtasks.collectAsStateWithLifecycle()

            MyApplicationTheme(
                isDark = when (themeMode) {
                    "DARK" -> true
                    "LIGHT" -> false
                    else -> false // Default to light for Earthy Sage theme
                }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 4.dp
                        ) {
                            // 1. Today Tab
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == AppScreen.TODAY) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday,
                                        contentDescription = "Today"
                                    )
                                },
                                label = { Text("Today") },
                                selected = currentScreen == AppScreen.TODAY,
                                onClick = { viewModel.navigateToScreen(AppScreen.TODAY) },
                                modifier = Modifier.testTag("nav_today"),
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                )
                            )

                            // 2. Stats Tab
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == AppScreen.STATS) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                        contentDescription = "Stats"
                                    )
                                },
                                label = { Text("Stats") },
                                selected = currentScreen == AppScreen.STATS,
                                onClick = { viewModel.navigateToScreen(AppScreen.STATS) },
                                modifier = Modifier.testTag("nav_stats"),
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                )
                            )

                            // 3. Settings Tab
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == AppScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings") },
                                selected = currentScreen == AppScreen.SETTINGS,
                                onClick = { viewModel.navigateToScreen(AppScreen.SETTINGS) },
                                modifier = Modifier.testTag("nav_settings"),
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        BotanicalBackgroundContainer(
                            showTopBranch = true,
                            showTopLeftBranch = true,
                            showBottomLeaf = true
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(ExpressiveMotionTokens.durationMedium)) togetherWith
                                            fadeOut(animationSpec = tween(ExpressiveMotionTokens.durationFast))
                                },
                                label = "screen_transition"
                            ) { screen ->
                                when (screen) {
                                    AppScreen.TODAY -> TodayScreen(
                                        viewModel = viewModel,
                                        use24Hour = use24Hour
                                    )
                                    AppScreen.STATS -> StatsScreen(
                                        viewModel = viewModel
                                    )
                                    AppScreen.SETTINGS -> SettingsScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }

                    // Global Quick Add Modal Sheet
                    QuickAddSheet(
                        isOpen = isQuickAddOpen,
                        defaultDate = selectedDate,
                        categories = categories,
                        onDismiss = { viewModel.closeQuickAdd() },
                        onSaveTask = { viewModel.saveTask(it) }
                    )

                    // Task Detail / Edit Modal Sheet
                    TaskEditorSheet(
                        task = editingTask,
                        defaultDate = selectedDate,
                        categories = categories,
                        existingSubtasks = editingTaskSubtasks,
                        use24Hour = use24Hour,
                        onDismiss = { viewModel.closeTaskEditor() },
                        onSave = { viewModel.saveTask(it) },
                        onSaveWithSubtasks = { task, subtasks ->
                            viewModel.saveTaskWithSubtasks(task, subtasks)
                        },
                        onDelete = { viewModel.deleteTask(it) },
                        onAddCategory = { name, color -> viewModel.addCategory(name, color) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NotificationHelper.restoreAllNotifications(this)
        com.example.widget.WidgetUpdater.updateAllWidgets(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra("ACTION_QUICK_ADD", false)) {
            viewModel.openQuickAdd()
        }

        if (intent.getStringExtra("NAVIGATE_TO") == "STATS") {
            viewModel.navigateToScreen(AppScreen.STATS)
        }

        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId != -1L) {
            lifecycleScope.launch {
                val db = com.example.data.AppDatabase.getInstance(this@MainActivity)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null) {
                    viewModel.openTaskEditor(task)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

