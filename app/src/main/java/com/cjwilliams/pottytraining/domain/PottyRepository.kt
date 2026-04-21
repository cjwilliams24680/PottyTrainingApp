package com.cjwilliams.pottytraining.domain

import kotlinx.coroutines.flow.Flow

interface PottyRepository {
    fun getLogs(): Flow<List<PottyLog>>
    suspend fun upsertLog(log: PottyLog)
    suspend fun deleteLog(log: PottyLog)
    suspend fun getLogById(id: Int): PottyLog?
}
