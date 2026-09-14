package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TaskItem::class, TaskCategory::class, Subtask::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        @Volatile
        private var testInstance: AppDatabase? = null

        fun setTestInstance(database: AppDatabase?) {
            testInstance = database
        }

        fun getInstance(context: Context): AppDatabase {
            testInstance?.let { return it }
            return INSTANCE ?: synchronized(this) {
                testInstance?.let { return it }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timeline_planner.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).taskDao()
                            dao.insertCategories(TaskCategory.DEFAULT_CATEGORIES)
                            // Add initial welcoming sample tasks to show the timeline elegance
                            val today = com.example.util.DateUtils.todayIso()
                            dao.insertTask(
                                TaskItem(
                                    title = "Morning planning & focus",
                                    note = "Review weekly priorities on the timeline",
                                    date = today,
                                    startTime = "09:00",
                                    category = "Focus",
                                    forwardIfIncomplete = true
                                )
                            )
                            dao.insertTask(
                                TaskItem(
                                    title = "Team alignment sync",
                                    note = "Discuss milestones and blockers",
                                    date = today,
                                    startTime = "10:30",
                                    endTime = "11:15",
                                    category = "Work",
                                    forwardIfIncomplete = true
                                )
                            )
                            dao.insertTask(
                                TaskItem(
                                    title = "Healthy lunch & walk",
                                    date = today,
                                    startTime = "12:30",
                                    category = "Health",
                                    forwardIfIncomplete = false
                                )
                            )
                            dao.insertTask(
                                TaskItem(
                                    title = "Deep work on product design",
                                    note = "Focus on minimal aesthetic tokens",
                                    date = today,
                                    startTime = "14:00",
                                    category = "Work",
                                    isPinned = true,
                                    forwardIfIncomplete = true
                                )
                            )
                            dao.insertTask(
                                TaskItem(
                                    title = "Review quarterly goals",
                                    date = today,
                                    category = "Personal",
                                    forwardIfIncomplete = true
                                )
                            )
                            dao.insertTask(
                                TaskItem(
                                    title = "Read 2 chapters of book",
                                    date = null, // Anytime
                                    category = "Personal"
                                )
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
