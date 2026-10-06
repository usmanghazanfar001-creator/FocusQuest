package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DailyChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenges WHERE date = :date ORDER BY isCompleted ASC, isClaimed ASC")
    fun getChallengesForDateFlow(date: String): Flow<List<DailyChallengeEntity>>

    @Query("SELECT * FROM daily_challenges WHERE date = :date")
    suspend fun getChallengesForDate(date: String): List<DailyChallengeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(challenges: List<DailyChallengeEntity>)

    @Update
    suspend fun update(challenge: DailyChallengeEntity)

    @Query("SELECT * FROM daily_challenges WHERE id = :id")
    suspend fun getChallengeById(id: String): DailyChallengeEntity?
}
