package com.cjwilliams.pottytraining.domain

import kotlinx.coroutines.flow.Flow

interface PottyRepository {
    fun getLogs(): Flow<List<PottyLog>>
    suspend fun upsertLog(log: PottyLog)
    suspend fun deleteLog(log: PottyLog)
    fun getLogById(id: Int): Flow<PottyLog?>
}
