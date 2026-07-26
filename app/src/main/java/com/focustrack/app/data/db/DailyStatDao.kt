package com.focustrack.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DailyStatDao {
    @Upsert
    suspend fun upsertAll(stats: List<DailyStatEntity>)

    @Upsert
    suspend fun upsert(stat: DailyStatEntity)

    @Query("SELECT * FROM daily_stats WHERE date >= :startDate ORDER BY date ASC")
    suspend fun since(startDate: String): List<DailyStatEntity>

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun get(date: String): DailyStatEntity?
}
