package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY completedAtMillis DESC")
    fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE completedAtMillis >= :sinceMillis ORDER BY completedAtMillis DESC")
    fun getSessionsSince(sinceMillis: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE sessionType = 'FOCUS'")
    fun getTotalFocusMinutesFlow(): Flow<Int?>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE sessionType = 'FOCUS' AND completedAtMillis >= :startOfDayMillis")
    fun getTodayFocusMinutesFlow(startOfDayMillis: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE sessionType = 'FOCUS'")
    fun getTotalFocusSessionsCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long
}
