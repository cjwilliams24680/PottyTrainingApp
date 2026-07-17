package com.cjwilliams.pottytraining.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-in for Room. [replaceAll] is inherited from [PottyDao] and exercises the
 * real deleteAll + insertLogs pair.
 */
class FakePottyDao : PottyDao {

    private val rows = MutableStateFlow<List<PottyEntity>>(emptyList())

    val current: List<PottyEntity> get() = rows.value

    override fun getLogs(): Flow<List<PottyEntity>> =
        rows.map { logs -> logs.sortedByDescending { it.timestamp } }

    override fun getLogById(id: String): Flow<PottyEntity?> =
        rows.map { logs -> logs.firstOrNull { it.id == id } }

    override suspend fun upsertLog(log: PottyEntity) {
        rows.value = rows.value.filterNot { it.id == log.id } + log
    }

    override suspend fun insertLogs(logs: List<PottyEntity>) {
        val incoming = logs.map { it.id }.toSet()
        rows.value = rows.value.filterNot { it.id in incoming } + logs
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

    override suspend fun deleteLog(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
