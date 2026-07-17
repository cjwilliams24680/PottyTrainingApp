package com.cjwilliams.pottytraining.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyType
import java.time.Instant

@Entity(tableName = "potty_logs")
data class PottyEntity(
    /** The server-assigned id. Rows only exist here after the server has accepted them. */
    @PrimaryKey val id: String,
    val timestamp: Long,
    val note: String?,
    val isAccident: Boolean,
    val type: String
)

fun PottyEntity.toDomain() = PottyLog(
    id = id,
    timestamp = Instant.ofEpochMilli(timestamp),
    note = note,
    isAccident = isAccident,
    type = PottyType.valueOf(type)
)

/**
 * Only logs the server has acknowledged get cached, so [PottyLog.id] is required here.
 */
fun PottyLog.toEntity() = PottyEntity(
    id = requireNotNull(id) { "Cannot cache a log that has no server id" },
    timestamp = timestamp.toEpochMilli(),
    note = note,
    isAccident = isAccident,
    type = type.name
)
