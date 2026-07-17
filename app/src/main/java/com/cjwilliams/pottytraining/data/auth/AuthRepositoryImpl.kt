package com.cjwilliams.pottytraining.data.auth

import com.cjwilliams.pottytraining.data.PottyDao
import com.cjwilliams.pottytraining.data.remote.AuthRemoteDataSource
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.AuthRepository
import com.cjwilliams.pottytraining.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val tokenStorage: TokenStorage,
    private val pottyDao: PottyDao
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = tokenStorage.tokens.map { it != null }

    override suspend fun login(email: String, password: String): AppResult<Unit> =
        remoteDataSource.login(email, password).persistSession()

    override suspend fun signup(email: String, password: String): AppResult<Unit> =
        remoteDataSource.signup(email, password).persistSession()

    /**
     * Tells the server first so it can revoke the refresh token, but the local session goes
     * either way — a failed call must not leave the user stuck signed in.
     */
    override suspend fun logout(): AppResult<Unit> {
        val result = remoteDataSource.logout()
        tokenStorage.clear()
        pottyDao.deleteAll()
        return result
    }

    override suspend fun currentUser(): AppResult<User> = remoteDataSource.me()

    private suspend fun AppResult<Tokens>.persistSession(): AppResult<Unit> = when (this) {
        is AppResult.Success -> {
            tokenStorage.update(data)
            AppResult.Success(Unit)
        }

        is AppResult.Error -> this
    }
}
