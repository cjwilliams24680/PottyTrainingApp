package com.cjwilliams.pottytraining.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyType

@Entity(tableName = "potty_logs")
data class PottyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val note: String,
    val isAccident: Boolean,
    val type: String
)

fun PottyEntity.toDomain() = PottyLog(
    id = id,
    timestamp = timestamp,
    note = note,
    isAccident = isAccident,
    type = PottyType.valueOf(type)
)

fun PottyLog.toEntity() = PottyEntity(
    id = id,
    timestamp = timestamp,
    note = note,
    isAccident = isAccident,
    type = type.name
)
