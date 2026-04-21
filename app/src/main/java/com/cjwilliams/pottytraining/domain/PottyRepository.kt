package com.cjwilliams.pottytraining.domain

import kotlinx.coroutines.flow.Flow

interface PottyRepository {
    fun getLogs(): Flow<List<PottyLog>>
    suspend fun addLog(log: PottyLog)
}
