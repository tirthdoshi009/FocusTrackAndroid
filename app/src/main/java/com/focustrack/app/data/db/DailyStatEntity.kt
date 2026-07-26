package com.focustrack.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey val date: String, // ISO-8601 local date, "yyyy-MM-dd"
    val totalMs: Long,
    val categorizedMs: Long,
    val riskyMs: Long,
    val productiveMs: Long,
    val neutralMs: Long,
    val uncategorizedMs: Long,
    val focusScore: Int,
)
