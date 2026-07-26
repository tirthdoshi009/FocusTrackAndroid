package com.focustrack.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DailyStatEntity::class], version = 1, exportSchema = false)
abstract class FocusTrackDatabase : RoomDatabase() {
    abstract fun dailyStatDao(): DailyStatDao

    companion object {
        @Volatile private var INSTANCE: FocusTrackDatabase? = null

        fun get(context: Context): FocusTrackDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusTrackDatabase::class.java,
                    "focustrack.db",
                ).build().also { INSTANCE = it }
            }
    }
}
