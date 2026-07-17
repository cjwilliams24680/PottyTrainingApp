package com.cjwilliams.pottytraining.domain

data class User(
    val id: String,
    val email: String,
    val emailVerified: Boolean
)
