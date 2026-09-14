package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.Subtask
import com.example.data.TaskCategory
import com.example.data.TaskItem
import com.example.data.TaskRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    TODAY,
    STATS,
    SETTINGS
}

data class TodayTimelineUiState(
    val selectedDate: String = DateUtils.todayIso(),
    val overdueTasks: List<TaskItem> = emptyList(),
    val timedTasks: List<TaskItem> = emptyList(),
    val dateOnlyTasks: List<TaskItem> = emptyList(),
    val unscheduledTasks: List<TaskItem> = emptyList(),
    val isToday: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = TaskRepository(application, database.taskDao())
    val prefs = PreferencesManager.getInstance(application)

    private val _currentScreen = MutableStateFlow(AppScreen.TODAY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedDate = MutableStateFlow(DateUtils.todayIso())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Sheet states
    private val _isQuickAddOpen = MutableStateFlow(false)
    val isQuickAddOpen: StateFlow<Boolean> = _isQuickAddOpen.asStateFlow()

    private val _editingTask = MutableStateFlow<TaskItem?>(null)
    val editingTask: StateFlow<TaskItem?> = _editingTask.asStateFlow()

    private val _lastCompletedTask = MutableStateFlow<TaskItem?>(null)
    val lastCompletedTask: StateFlow<TaskItem?> = _lastCompletedTask.asStateFlow()

    val categories: StateFlow<List<TaskCategory>> = repository.allCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedTasks: StateFlow<List<TaskItem>> = repository.activePinnedTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskItem>> = repository.allTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubtasks: StateFlow<List<Subtask>> = repository.getAllSubtasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val editingTaskSubtasks: StateFlow<List<Subtask>> = _editingTask.flatMapLatest { task ->
        if (task != null && task.id != 0L) {
            repository.getSubtasksForTaskFlow(task.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combine tasks for today's timeline
    val timelineState: StateFlow<TodayTimelineUiState> = combine(
        _selectedDate,
        repository.allTasksFlow
    ) { date, all ->
        val isSelectedToday = DateUtils.isToday(date)
        val isFuture = date > DateUtils.todayIso()

        // Overdue tasks: only show incomplete tasks originated on previous dates AND forwardIfIncomplete == true
        // Only show overdue when viewing today or future
        val overdue = if (isSelectedToday || isFuture) {
            all.filter {
                !it.isCompleted &&
                it.date != null &&
                it.date < date &&
                it.forwardIfIncomplete
            }.sortedBy { it.date }
        } else {
            emptyList()
        }

        // Tasks assigned to this date
        val forThisDate = all.filter { it.date == date }

        // Timed tasks sorted by startTime
        val timed = forThisDate.filter { it.startTime != null }
            .sortedBy { it.startTime }

        // Date-only tasks
        val dateOnly = forThisDate.filter { it.startTime == null }
            .sortedBy { it.orderIndex }

        // Anytime / Unscheduled tasks (date == null)
        val unscheduled = all.filter { it.date == null }
            .sortedBy { it.orderIndex }

        TodayTimelineUiState(
            selectedDate = date,
            overdueTasks = overdue,
            timedTasks = timed,
            dateOnlyTasks = dateOnly,
            unscheduledTasks = unscheduled,
            isToday = isSelectedToday
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TodayTimelineUiState()
    )

    fun navigateToScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun goToToday() {
        _selectedDate.value = DateUtils.todayIso()
    }

    fun offsetSelectedDate(days: Int) {
        _selectedDate.value = DateUtils.offsetDate(_selectedDate.value, days)
    }

    fun openQuickAdd() {
        _isQuickAddOpen.value = true
    }

    fun closeQuickAdd() {
        _isQuickAddOpen.value = false
    }

    fun openTaskEditor(task: TaskItem? = null) {
        _editingTask.value = task
    }

    fun closeTaskEditor() {
        _editingTask.value = null
    }

    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            _lastCompletedTask.value = task
            repository.toggleCompletion(task)
            com.example.widget.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun undoCompletion() {
        val task = _lastCompletedTask.value ?: return
        viewModelScope.launch {
            repository.toggleCompletion(task)
            _lastCompletedTask.value = null
            com.example.widget.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun clearLastCompleted() {
        _lastCompletedTask.value = null
    }

    fun togglePin(task: TaskItem) {
        viewModelScope.launch {
            repository.togglePin(task)
            com.example.widget.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun saveTask(task: TaskItem) {
        viewModelScope.launch {
            repository.saveTask(task)
            closeQuickAdd()
            closeTaskEditor()
            com.example.widget.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun saveTaskWithSubtasks(task: TaskItem, subtasks: List<Subtask>) {
        viewModelScope.launch {
            val taskId = repository.saveTask(task)
            repository.saveSubtasks(taskId, subtasks)
            closeQuickAdd()
            closeTaskEditor()
            com.example.widget.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun toggleSubtaskCompletion(subtaskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleSubtaskCompletion(subtaskId, isCompleted)
            com.example.widget.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task)
            closeTaskEditor()
            com.example.widget.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun forwardOverdueTask(task: TaskItem) {
        viewModelScope.launch {
            repository.forwardTaskToDate(task, _selectedDate.value)
            com.example.widget.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.addCategory(TaskCategory(name = name, colorHex = colorHex, isCustom = true))
        }
    }
}
