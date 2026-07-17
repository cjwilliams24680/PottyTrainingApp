package com.cjwilliams.pottytraining.domain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Stands in for the real repository, which needs Room and Apollo. The cache is set directly with
 * [setLogs] rather than by [refreshLogs], so tests can stage "server returned these" and "the
 * refresh failed" independently the way the real offline-tolerant repository allows.
 */
class FakePottyRepository : PottyRepository {

    private val logs = MutableStateFlow<List<PottyLog>>(emptyList())

    /** What the next [refreshLogs] returns. */
    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
    var refreshCallCount = 0

    /** Set to hold [refreshLogs] open so a test can observe the in-flight window. */
    var refreshGate: CompletableDeferred<Unit>? = null

    /** What [deleteLog] returns. */
    var deleteResult: AppResult<Unit> = AppResult.Success(Unit)
    var deletedLogs = mutableListOf<PottyLog>()

    /** When set, [saveLog] fails with this error instead of echoing the log back. */
    var saveError: AppError? = null
    var savedLogs = mutableListOf<PottyLog>()

    override fun getLogs(): Flow<List<PottyLog>> = logs

    override fun getLogById(id: String): Flow<PottyLog?> = MutableStateFlow(logs.value.firstOrNull { it.id == id })

    override suspend fun refreshLogs(): AppResult<Unit> {
        refreshCallCount++
        refreshGate?.await()
        return refreshResult
    }

    override suspend fun saveLog(log: PottyLog): AppResult<PottyLog> {
        savedLogs += log
        return saveError?.let { AppResult.Error(it) } ?: AppResult.Success(log)
    }

    override suspend fun deleteLog(log: PottyLog): AppResult<Unit> {
        deletedLogs += log
        return deleteResult
    }

    fun setLogs(value: List<PottyLog>) {
        logs.value = value
    }
}
