package com.cjwilliams.pottytraining.domain

import kotlinx.coroutines.flow.Flow

data class PottyLog(
    val id: Int = 0,
    val timestamp: Long,
    val note: String = ""
)

interface PottyRepository {
    fun getLogs(): Flow<List<PottyLog>>
    suspend fun addLog(log: PottyLog)
}
