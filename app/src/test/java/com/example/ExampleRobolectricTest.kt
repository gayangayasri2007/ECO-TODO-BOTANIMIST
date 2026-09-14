package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.RepeatRule
import com.example.data.Subtask
import com.example.data.TaskDao
import com.example.data.TaskItem
import com.example.notifications.NotificationEngine
import com.example.notifications.TaskNotificationReceiver
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.taskDao()
        AppDatabase.setTestInstance(db)
    }

    @After
    fun tearDown() {
        try {
            db.close()
        } catch (_: Throwable) {}
        AppDatabase.setTestInstance(null)
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Eco Todo: Botanimist", appName)
    }

    @Test
    fun `insert and retrieve task item`() = runBlocking {
        val task = TaskItem(
            title = "Morning Deep Work",
            date = "2026-09-08",
            startTime = "09:00",
            endTime = "10:30",
            category = "Work",
            isPinned = true,
            forwardIfIncomplete = true
        )
        val id = dao.insertTask(task)
        assertTrue(id > 0)

        val retrieved = dao.getTaskById(id)
        assertNotNull(retrieved)
        assertEquals("Morning Deep Work", retrieved?.title)
        assertEquals("09:00", retrieved?.startTime)
        assertTrue(retrieved?.isPinned == true)
        assertFalse(retrieved?.isCompleted == true)
    }

    @Test
    fun `insert and toggle subtasks independently`() = runBlocking {
        val task = TaskItem(title = "Project Launch", date = "2026-09-08")
        val taskId = dao.insertTask(task)

        val subtask1 = Subtask(taskId = taskId, title = "Write docs", orderIndex = 0)
        val subtask2 = Subtask(taskId = taskId, title = "Deploy build", orderIndex = 1)
        dao.insertSubtasks(listOf(subtask1, subtask2))

        val retrieved = dao.getSubtasksForTask(taskId)
        assertEquals(2, retrieved.size)
        assertFalse(retrieved[0].isCompleted)

        dao.updateSubtaskCompletion(retrieved[0].id, true, System.currentTimeMillis())
        val updated = dao.getSubtasksForTask(taskId)
        assertTrue(updated[0].isCompleted)
        assertNotNull(updated[0].completedAt)
        assertFalse(updated[1].isCompleted)

        // Parent task remains incomplete
        val parentTask = dao.getTaskById(taskId)
        assertFalse(parentTask?.isCompleted == true)
    }

    @Test
    fun `forwardable incomplete tasks are retrieved for today`() = runBlocking {
        // Task from yesterday that is incomplete and forwardable
        val pastTask = TaskItem(
            title = "Unfinished yesterday",
            date = "2026-09-07",
            isCompleted = false,
            forwardIfIncomplete = true
        )
        dao.insertTask(pastTask)

        // Task from yesterday that is completed
        val pastCompleted = TaskItem(
            title = "Finished yesterday",
            date = "2026-09-07",
            isCompleted = true,
            forwardIfIncomplete = true
        )
        dao.insertTask(pastCompleted)

        val forwardable = dao.getForwardableIncompleteTasksBeforeFlow("2026-09-08").first()
        assertEquals(1, forwardable.size)
        assertEquals("Unfinished yesterday", forwardable.first().title)
    }

    @Test
    fun `recurring task date calculation works accurately`() {
        val dailyNext = NotificationEngine.computeNextRecurringDate("2026-09-08", RepeatRule.DAILY)
        assertEquals("2026-09-09", dailyNext)

        val weeklyNext = NotificationEngine.computeNextRecurringDate("2026-09-08", RepeatRule.WEEKLY)
        assertEquals("2026-09-15", weeklyNext)
    }

    @Test
    fun `date utils weekday generation covers 7 days`() {
        val days = DateUtils.getWeekDays("2026-09-08")
        assertEquals(7, days.size)
        assertTrue(days.contains("2026-09-08"))
    }

    @Test
    fun `earthy sage dark theme provides requested tokens`() {
        val result = com.example.ui.theme.getThemeColorScheme("Earthy Sage", true)
        val colorScheme = result.first
        val tokens = result.second
        assertEquals(com.example.ui.theme.EarthySageDarkPrimary, colorScheme.primary)
        assertEquals(com.example.ui.theme.EarthySageDarkBackground, colorScheme.background)
        assertEquals(com.example.ui.theme.EarthySageDarkSurface, colorScheme.surface)
        assertEquals(com.example.ui.theme.EarthySageDarkSurfaceVariant, colorScheme.surfaceVariant)
        assertEquals(com.example.ui.theme.EarthySageDarkOnBackground, colorScheme.onBackground)
        assertEquals(com.example.ui.theme.EarthySageDarkTimelineAccentGlow, tokens.accentGlow)
    }

    @Test
    fun `earthy sage light theme provides requested tokens`() {
        val result = com.example.ui.theme.getThemeColorScheme("Earthy Sage", false)
        val colorScheme = result.first
        val tokens = result.second
        assertEquals(com.example.ui.theme.EarthySagePrimary, colorScheme.primary)
        assertEquals(com.example.ui.theme.EarthySageBackground, colorScheme.background)
        assertEquals(com.example.ui.theme.EarthySageSurface, colorScheme.surface)
        assertEquals(com.example.ui.theme.EarthySageSurfaceVariant, colorScheme.surfaceVariant)
        assertEquals(com.example.ui.theme.EarthySageOnBackground, colorScheme.onBackground)
        assertEquals(com.example.ui.theme.EarthySageTimelineAccentGlow, tokens.accentGlow)
    }

    @Test
    fun `widget updater executes without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.widget.WidgetUpdater.updateAllWidgets(context)
        com.example.widget.WidgetUpdater.scheduleMidnightRollover(context)
    }

    @Test
    fun `today widget factory flattens tasks and subtasks correctly when expanded`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testTask = TaskItem(title = "Task with Subtasks", date = DateUtils.todayIso())
        val taskId = dao.insertTask(testTask)

        val sub1 = Subtask(taskId = taskId, title = "Subtask 1", isCompleted = false)
        val sub2 = Subtask(taskId = taskId, title = "Subtask 2", isCompleted = true)
        dao.insertSubtasks(listOf(sub1, sub2))

        // Set up the expanded state in SharedPreferences
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().putStringSet("expanded_tasks", HashSet(listOf(taskId.toString()))).commit()

        val factory = com.example.widget.TodayWidgetFactory(context)
        factory.onDataSetChanged()

        // Expecting at least 3 items in the list: the parent task followed by subtasks
        assertTrue("Widget should have at least 3 items, actual: ${factory.getCount()}", factory.getCount() >= 3)

        val firstItemViews = factory.getViewAt(0)
        assertNotNull(firstItemViews)

        val secondItemViews = factory.getViewAt(1)
        assertNotNull(secondItemViews)
    }

    @Test
    fun `botanical accents manager provides correct resources and contrast hierarchy`() {
        assertEquals(1.0f, com.example.ui.components.BotanicalContrast.FOREGROUND, 0.001f)
        assertEquals(0.85f, com.example.ui.components.BotanicalContrast.SECONDARY, 0.001f)
        assertEquals(0.22f, com.example.ui.components.BotanicalContrast.BACKGROUND_SILHOUETTE, 0.001f)
        assertEquals(0.40f, com.example.ui.components.BotanicalContrast.CORNER_BRANCH, 0.001f)
        assertEquals(0.35f, com.example.ui.components.BotanicalContrast.FAINT_STEM, 0.001f)

        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.TopCornerBranch)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.TimelineEdgeStem)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.EmptySpaceSilhouette)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.StatsComposition)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.BackgroundSilhouette)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.WidgetAccent)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.EmptyStateIllustration)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.EmptyStateArtwork)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.HeroCard)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.HeroBanner)
        assertNotEquals(0, com.example.ui.components.BotanicalDrawables.QuickAddDetail)

        // BotanicalAssets categorized registry verification
        assertNotEquals(0, com.example.ui.components.BotanicalAssets.Background.topBranch)
        assertNotEquals(0, com.example.ui.components.BotanicalAssets.Background.timelineStem)
        assertNotEquals(0, com.example.ui.components.BotanicalAssets.Background.leafSilhouette)
        assertNotEquals(0, com.example.ui.components.BotanicalAssets.Background.emptySpaceSilhouette)
        assertNotEquals(0, com.example.ui.components.BotanicalAssets.Accent.widget)
        assertNotEquals(0, com.example.ui.components.BotanicalAssets.Accent.quickAdd)
        assertNotEquals(0, com.example.ui.components.BotanicalAssets.Illustration.statsComposition)
        assertNotEquals(0, com.example.ui.components.BotanicalAssets.Illustration.emptyState)

        // BotanicalUsageType default opacity and z-index contract verification
        assertEquals(-2f, com.example.ui.components.BotanicalUsageType.BACKGROUND_SILHOUETTE.defaultZIndex, 0.01f)
        assertEquals(0.22f, com.example.ui.components.BotanicalUsageType.BACKGROUND_SILHOUETTE.defaultAlpha, 0.001f)
        assertEquals(-1f, com.example.ui.components.BotanicalUsageType.TIMELINE_STEM.defaultZIndex, 0.01f)
        assertEquals(0.35f, com.example.ui.components.BotanicalUsageType.TIMELINE_STEM.defaultAlpha, 0.001f)
        assertEquals(-1f, com.example.ui.components.BotanicalUsageType.CORNER_BRANCH.defaultZIndex, 0.01f)
        assertEquals(0.40f, com.example.ui.components.BotanicalUsageType.CORNER_BRANCH.defaultAlpha, 0.001f)
        assertEquals(0f, com.example.ui.components.BotanicalUsageType.ILLUSTRATION.defaultZIndex, 0.01f)
        assertEquals(0.85f, com.example.ui.components.BotanicalUsageType.ILLUSTRATION.defaultAlpha, 0.001f)
    }

    @Test
    fun `pinned notification builder configures non-dismissible ongoing properties and custom interactive RemoteViews`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val task = TaskItem(
            id = 42L,
            title = "Focus on Critical Bug",
            date = "2026-09-08",
            startTime = "10:00",
            category = "Engineering",
            isPinned = true
        )
        val subtasks = listOf(
            Subtask(id = 101L, taskId = 42L, title = "Reproduce bug", isCompleted = true),
            Subtask(id = 102L, taskId = 42L, title = "Write fix", isCompleted = false)
        )

        val notification = NotificationEngine.buildPinnedNotification(context, task, subtasks)

        // 1. Ongoing and non-dismissible flags
        assertTrue(
            "FLAG_ONGOING_EVENT must be present on pinned notification",
            (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        )
        assertTrue(
            "FLAG_NO_CLEAR must be present on pinned notification",
            (notification.flags and android.app.Notification.FLAG_NO_CLEAR) != 0
        )
        assertFalse(
            "FLAG_AUTO_CANCEL must NOT be present on pinned notification",
            (notification.flags and android.app.Notification.FLAG_AUTO_CANCEL) != 0
        )

        // 2. Channel check
        assertEquals(NotificationEngine.CHANNEL_PINNED, notification.channelId)

        // 3. Delete intent configured for re-asserting ongoing state if swiped on API 34+
        assertNotNull("Delete intent must be set for ongoing persistence", notification.deleteIntent)

        // 4. Custom RemoteViews with inline parent checkbox and subtasks
        assertNotNull("Custom contentView must be configured for inline checkbox", notification.contentView)
        assertNotNull("Custom bigContentView must be configured for expanded checklist", notification.bigContentView)
    }

    @Test
    fun `subtask toggle action receiver updates subtask without completing parent task`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val task = TaskItem(title = "Parent Project", date = DateUtils.todayIso(), isPinned = true)
        val taskId = dao.insertTask(task)

        val subtask = Subtask(taskId = taskId, title = "Draft Specs", isCompleted = false)
        val subtaskId = dao.insertSubtask(subtask)
        val subtask2 = Subtask(taskId = taskId, title = "Final Review", isCompleted = false)
        dao.insertSubtask(subtask2)

        // Execute subtask toggle
        NotificationEngine.handleToggleSubtask(context, subtaskId, taskId)

        val updatedSubtask = dao.getSubtaskById(subtaskId)
        assertNotNull(updatedSubtask)
        assertTrue("Subtask must be marked completed", updatedSubtask!!.isCompleted)

        val parentTask = dao.getTaskById(taskId)
        assertNotNull(parentTask)
        assertFalse("Parent task must remain incomplete when only subtask is toggled", parentTask!!.isCompleted)
    }

    @Test
    fun `parent complete action marks parent done while preserving subtasks`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val task = TaskItem(title = "Major Release", date = DateUtils.todayIso(), isPinned = true)
        val taskId = dao.insertTask(task)

        val sub1 = Subtask(taskId = taskId, title = "Step A", isCompleted = true)
        val sub2 = Subtask(taskId = taskId, title = "Step B", isCompleted = false)
        dao.insertSubtasks(listOf(sub1, sub2))

        // Trigger mark done
        NotificationEngine.handleMarkDone(context, taskId)

        val completedParent = dao.getTaskById(taskId)
        assertNotNull(completedParent)
        assertTrue("Parent task must be marked completed", completedParent!!.isCompleted)

        val preservedSubtasks = dao.getSubtasksForTask(taskId)
        assertEquals(2, preservedSubtasks.size)
        assertTrue(preservedSubtasks.any { it.title == "Step A" && it.isCompleted })
        assertTrue(preservedSubtasks.any { it.title == "Step B" && !it.isCompleted })
    }

    @Test
    fun `reminder alert notification is separate, high importance, and dismissible`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val task = TaskItem(
            id = 42L,
            title = "Standup Meeting",
            date = "2026-09-08",
            startTime = "09:30",
            reminderMinutesBefore = 10
        )

        val notification = NotificationEngine.buildReminderAlertNotification(context, task)

        // 1. Must be dismissible and auto-canceling
        assertTrue(
            "FLAG_AUTO_CANCEL must be set on reminder notification",
            (notification.flags and android.app.Notification.FLAG_AUTO_CANCEL) != 0
        )
        assertFalse(
            "FLAG_ONGOING_EVENT must NOT be set on reminder notification",
            (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        )

        // 2. Channel check
        assertEquals(NotificationEngine.CHANNEL_REMINDERS, notification.channelId)

        // 3. Distinct ID separation check
        val pinnedId = NotificationEngine.getPinnedNotificationId(42L)
        val reminderId = NotificationEngine.getReminderAlertNotificationId(42L)
        assertTrue("Pinned and Reminder notification IDs must be distinct", pinnedId != reminderId)
    }

    @Test
    fun `completing and unpinning task manages pinned notification lifecycle cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.TaskRepository(context, dao)

        val task = TaskItem(
            title = "Important Review",
            date = "2026-09-08",
            isPinned = true
        )
        val id = repo.saveTask(task)
        val saved = repo.getTaskById(id)!!
        assertTrue(saved.isPinned)
        assertFalse(saved.isCompleted)

        // Complete the task via repo
        repo.toggleCompletion(saved)
        val completed = repo.getTaskById(id)!!
        assertTrue(completed.isCompleted)

        // Unpin via repo
        repo.togglePin(saved)
        val unpinned = repo.getTaskById(id)!!
        assertFalse(unpinned.isPinned)
    }

    @Test
    fun `botanical assets and contrast levels are configured with visible highlights`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Verify drawables exist and load without exception
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, com.example.ui.components.BotanicalDrawables.HeroCard))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, com.example.ui.components.BotanicalDrawables.HeroBanner))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, com.example.ui.components.BotanicalDrawables.EmptyStateArtwork))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, com.example.ui.components.BotanicalDrawables.TimelineEdgeStem))

        // Verify contrast values provide clear visibility
        assertTrue(com.example.ui.components.BotanicalContrast.CORNER_BRANCH >= 0.30f)
        assertTrue(com.example.ui.components.BotanicalContrast.FAINT_STEM >= 0.25f)
        assertTrue(com.example.ui.components.BotanicalContrast.BACKGROUND_SILHOUETTE >= 0.15f)
        assertTrue(com.example.ui.components.BotanicalContrast.SECONDARY >= 0.70f)
    }
}
