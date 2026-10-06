package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RewardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards ORDER BY costCoins ASC")
    fun getAllRewardsFlow(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards WHERE id = :id")
    suspend fun getRewardById(id: String): RewardEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rewards: List<RewardEntity>)

    @Update
    suspend fun update(reward: RewardEntity)

    @Query("UPDATE rewards SET isEquipped = 0 WHERE type = :type")
    suspend fun unequipAllOfType(type: String)

    @Query("UPDATE rewards SET isEquipped = 1 WHERE id = :id")
    suspend fun equipReward(id: String)
}
