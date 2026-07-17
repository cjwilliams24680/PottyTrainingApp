package com.cjwilliams.pottytraining.domain

import kotlinx.coroutines.flow.Flow

interface PottyRepository {
    /** Observes the locally cached logs. Call [refreshLogs] to pull the latest from the server. */
    fun getLogs(): Flow<List<PottyLog>>

    fun getLogById(id: String): Flow<PottyLog?>

    /** Refetches every log from the server and replaces the cache. Leaves the cache alone on failure. */
    suspend fun refreshLogs(): AppResult<Unit>

    /** Creates the log when [PottyLog.id] is null, otherwise updates it. Returns the saved server copy. */
    suspend fun saveLog(log: PottyLog): AppResult<PottyLog>

    suspend fun deleteLog(log: PottyLog): AppResult<Unit>
}
