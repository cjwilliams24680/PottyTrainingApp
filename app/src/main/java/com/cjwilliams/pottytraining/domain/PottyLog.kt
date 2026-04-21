package com.cjwilliams.pottytraining.domain

enum class PottyType {
    PEE, POO, BOTH
}

data class PottyLog(
    val id: Int = 0,
    val timestamp: Long,
    val note: String = "",
    val isAccident: Boolean = false,
    val type: PottyType = PottyType.PEE
)
