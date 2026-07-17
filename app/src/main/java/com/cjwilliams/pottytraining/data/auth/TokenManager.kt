package com.cjwilliams.pottytraining.data.auth

import com.apollographql.apollo.ApolloClient
import com.cjwilliams.pottytraining.data.remote.toAppResult
import com.cjwilliams.pottytraining.di.UnauthenticatedApollo
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.graphql.RefreshMutation
import com.cjwilliams.pottytraining.graphql.type.RefreshInput
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Owns access token renewal.
 *
 * The client is injected lazily via [Provider] because the unauthenticated client is built
 * in the same Hilt module as the authenticated one that depends on this class.
 */
@Singleton
class TokenManager @Inject constructor(
    @UnauthenticatedApollo private val apolloClient: Provider<ApolloClient>,
    private val tokenStorage: TokenStorage
) : TokenRefresher {
    private val mutex = Mutex()

    override suspend fun accessToken(): String? = tokenStorage.current()?.accessToken

    /**
     * Trades the refresh token for a fresh pair and returns the new access token, or null
     * if the session could not be renewed.
     *
     * Concurrent callers share a single refresh: each passes the access token it just tried,
     * and anyone arriving after a refresh already happened gets the new token straight back
     * instead of spending the refresh token a second time.
     */
    override suspend fun refresh(usedAccessToken: String?): String? = mutex.withLock {
        val tokens = tokenStorage.current() ?: return@withLock null

        if (usedAccessToken != null && tokens.accessToken != usedAccessToken) {
            return@withLock tokens.accessToken
        }

        val result = apolloClient.get()
            .mutation(RefreshMutation(RefreshInput(refreshToken = tokens.refreshToken)))
            .execute()
            .toAppResult { it.refresh.authPayloadFields }

        when (result) {
            is AppResult.Success -> {
                val payload = result.data
                tokenStorage.update(Tokens(payload.accessToken, payload.refreshToken))
                payload.accessToken
            }

            is AppResult.Error -> {
                // Only a rejected refresh token means the session is genuinely over. Dropping
                // it on a network blip would sign the user out for being briefly offline.
                if (result.error is AppError.Auth) {
                    Timber.w("Refresh token rejected, clearing session")
                    tokenStorage.clear()
                } else {
                    Timber.w("Token refresh failed: %s", result.error)
                }
                null
            }
        }
    }
}
