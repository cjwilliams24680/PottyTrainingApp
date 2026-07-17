package com.cjwilliams.pottytraining.domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Whether a session is stored locally. It says nothing about the server still honouring it. */
    val isLoggedIn: Flow<Boolean>

    suspend fun login(email: String, password: String): AppResult<Unit>

    suspend fun signup(email: String, password: String): AppResult<Unit>

    /** Clears the local session and cached logs even if the server call fails. */
    suspend fun logout(): AppResult<Unit>

    suspend fun currentUser(): AppResult<User>
}
