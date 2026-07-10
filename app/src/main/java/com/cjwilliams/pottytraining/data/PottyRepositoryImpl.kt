package com.cjwilliams.pottytraining.data

import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PottyRepositoryImpl @Inject constructor(
    private val pottyDao: PottyDao
) : PottyRepository {
    override fun getLogs(): Flow<List<PottyLog>> {
        return pottyDao.getLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsertLog(log: PottyLog) {
        pottyDao.upsertLog(log.toEntity())
    }

    override suspend fun deleteLog(log: PottyLog) {
        pottyDao.deleteLog(log.id)
    }

    override fun getLogById(id: Int): Flow<PottyLog?> {
        return pottyDao.getLogById(id).map { it?.toDomain() }
    }
}
