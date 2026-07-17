package com.cjwilliams.pottytraining.domain

import java.time.Instant

enum class PottyType {
    PEE, POO, BOTH
}

data class PottyLog(
    /** The server-assigned id. Null until the log has been created on the server. */
    val id: String? = null,
    val timestamp: Instant,
    val note: String? = null,
    val isAccident: Boolean = false,
    val type: PottyType = PottyType.PEE
)
