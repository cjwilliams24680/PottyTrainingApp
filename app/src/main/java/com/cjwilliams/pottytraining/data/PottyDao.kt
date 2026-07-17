package com.cjwilliams.pottytraining.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PottyDao {
    @Query("SELECT * FROM potty_logs ORDER BY timestamp DESC")
    fun getLogs(): Flow<List<PottyEntity>>

    @Query("SELECT * FROM potty_logs WHERE id = :id")
    fun getLogById(id: String): Flow<PottyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: PottyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<PottyEntity>)

    @Query("DELETE FROM potty_logs")
    suspend fun deleteAll()

    @Query("DELETE FROM potty_logs WHERE id = :id")
    suspend fun deleteLog(id: String)

    /** Swaps the cache for [logs] in one transaction so observers never see an empty list. */
    @Transaction
    suspend fun replaceAll(logs: List<PottyEntity>) {
        deleteAll()
        insertLogs(logs)
    }
}
