package com.cjwilliams.pottytraining.data.remote

import com.apollographql.apollo.api.Optional
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyType
import com.cjwilliams.pottytraining.graphql.fragment.PottyLogFields
import com.cjwilliams.pottytraining.graphql.type.CreateLogInput
import com.cjwilliams.pottytraining.graphql.type.UpdateLogInput
import timber.log.Timber
import com.cjwilliams.pottytraining.graphql.type.PottyType as ApolloPottyType

/**
 * Returns null when the server sent a [PottyType] this build doesn't know about. Guessing a
 * fallback would show the wrong event, so such a log is dropped by the caller instead.
 */
internal fun PottyLogFields.toDomain(): PottyLog? {
    val domainType = type.toDomain()
    if (domainType == null) {
        Timber.w("Dropping log %s with unrecognized type '%s'", id, type.rawValue)
        return null
    }
    return PottyLog(
        id = id,
        timestamp = timestamp,
        note = note,
        isAccident = isAccident,
        type = domainType
    )
}

internal fun List<PottyLogFields>.toDomain(): List<PottyLog> = mapNotNull { it.toDomain() }

internal fun PottyLog.toCreateInput() = CreateLogInput(
    isAccident = isAccident,
    note = Optional.presentIfNotNull(note),
    timestamp = timestamp,
    type = type.toApollo()
)

/**
 * Every field is sent as [Optional.Present] because a [PottyLog] is a full snapshot rather
 * than a patch. It matters for [PottyLog.note]: `Present(null)` clears it server-side, while
 * `Absent` would leave the old note in place when the user has just erased it.
 */
internal fun PottyLog.toUpdateInput(id: String) = UpdateLogInput(
    id = id,
    isAccident = Optional.Present(isAccident),
    note = Optional.Present(note),
    timestamp = Optional.Present(timestamp),
    type = Optional.Present(type.toApollo())
)

internal fun ApolloPottyType.toDomain(): PottyType? = when (this) {
    ApolloPottyType.PEE -> PottyType.PEE
    ApolloPottyType.POO -> PottyType.POO
    ApolloPottyType.BOTH -> PottyType.BOTH
    is ApolloPottyType.UNKNOWN__ -> null
}

internal fun PottyType.toApollo(): ApolloPottyType = when (this) {
    PottyType.PEE -> ApolloPottyType.PEE
    PottyType.POO -> ApolloPottyType.POO
    PottyType.BOTH -> ApolloPottyType.BOTH
}
