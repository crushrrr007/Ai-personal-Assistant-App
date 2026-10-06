package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room Database for the Assistant application.
 * Manages local storage for Tasks, Daily Plans, Notes, Chat Messages, and Planning Feedback.
 */
@Database(
    entities = [
        User::class,
        Task::class,
        PlanBlock::class,
        DailyPlanEntity::class,
        Note::class,
        ChatMessage::class,
        PlanFeedback::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun planDao(): PlanDao
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun noteDao(): NoteDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "assistant_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
