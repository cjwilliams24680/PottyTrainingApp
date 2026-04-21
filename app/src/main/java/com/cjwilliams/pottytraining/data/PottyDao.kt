package com.cjwilliams.pottytraining.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PottyDao {
    @Query("SELECT * FROM potty_logs ORDER BY timestamp DESC")
    fun getLogs(): Flow<List<PottyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: PottyEntity)

    @Query("DELETE FROM potty_logs WHERE id = :id")
    suspend fun deleteLog(id: Int)

    @Query("SELECT * FROM potty_logs WHERE id = :id")
    suspend fun getLogById(id: Int): PottyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateLog(log: PottyEntity)
}
