package com.cjwilliams.pottytraining.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cjwilliams.pottytraining.domain.PottyLog

@Entity(tableName = "potty_logs")
data class PottyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val note: String
)

fun PottyEntity.toDomain() = PottyLog(
    id = id,
    timestamp = timestamp,
    note = note
)

fun PottyLog.toEntity() = PottyEntity(
    id = id,
    timestamp = timestamp,
    note = note
)
