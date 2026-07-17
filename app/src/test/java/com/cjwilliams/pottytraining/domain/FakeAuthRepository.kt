package com.cjwilliams.pottytraining.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Stands in for the real repository, which needs DataStore and Apollo. Signing in flips
 * [isLoggedIn] the way token storage does, so callers can observe the session change.
 */
class FakeAuthRepository : AuthRepository {

    private val loggedIn = MutableStateFlow(false)

    override val isLoggedIn: Flow<Boolean> = loggedIn

    /** What the next [login] or [signup] returns. */
    var result: AppResult<Unit> = AppResult.Success(Unit)

    var loginCalls = mutableListOf<Pair<String, String>>()
    var signupCalls = mutableListOf<Pair<String, String>>()
    var logoutCount = 0

    override suspend fun login(email: String, password: String): AppResult<Unit> {
        loginCalls += email to password
        return result.alsoUpdateSession()
    }

    override suspend fun signup(email: String, password: String): AppResult<Unit> {
        signupCalls += email to password
        return result.alsoUpdateSession()
    }

    override suspend fun logout(): AppResult<Unit> {
        logoutCount++
        loggedIn.value = false
        return AppResult.Success(Unit)
    }

    override suspend fun currentUser(): AppResult<User> =
        AppResult.Success(User(id = "user-1", email = "test@example.com", emailVerified = true))

    fun setLoggedIn(value: Boolean) {
        loggedIn.value = value
    }

    private fun AppResult<Unit>.alsoUpdateSession() = also {
        if (it is AppResult.Success) loggedIn.value = true
    }
}
