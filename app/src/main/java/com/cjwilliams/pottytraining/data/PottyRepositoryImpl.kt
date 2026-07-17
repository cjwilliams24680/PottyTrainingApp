package com.cjwilliams.pottytraining.data

import com.cjwilliams.pottytraining.data.remote.PottyRemoteDataSource
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Reads come from the Room cache; writes go to the server first and the cache is updated
 * from what the server returns, so the two can't drift apart.
 */
class PottyRepositoryImpl @Inject constructor(
    private val pottyDao: PottyDao,
    private val remoteDataSource: PottyRemoteDataSource
) : PottyRepository {

    override fun getLogs(): Flow<List<PottyLog>> =
        pottyDao.getLogs().map { entities -> entities.map { it.toDomain() } }

    override fun getLogById(id: String): Flow<PottyLog?> =
        pottyDao.getLogById(id).map { it?.toDomain() }

    override suspend fun refreshLogs(): AppResult<Unit> =
        when (val result = remoteDataSource.getLogs()) {
            is AppResult.Success -> {
                pottyDao.replaceAll(result.data.map { it.toEntity() })
                AppResult.Success(Unit)
            }
            // The cache stays as it was; stale logs beat an empty history screen.
            is AppResult.Error -> result
        }

    override suspend fun saveLog(log: PottyLog): AppResult<PottyLog> {
        val id = log.id
        val result = if (id == null) {
            remoteDataSource.createLog(log)
        } else {
            remoteDataSource.updateLog(id, log)
        }

        if (result is AppResult.Success) {
            pottyDao.upsertLog(result.data.toEntity())
        }
        return result
    }

    override suspend fun deleteLog(log: PottyLog): AppResult<Unit> {
        // TODO: wire this up once PottyTrainingServer exposes a deleteLog mutation. Deleting
        // only the cached row would just bring the log back on the next refresh.
        Timber.w("deleteLog(%s) ignored: the server has no delete mutation yet", log.id)
        return AppResult.Error(AppError.NotSupported)
    }
}
