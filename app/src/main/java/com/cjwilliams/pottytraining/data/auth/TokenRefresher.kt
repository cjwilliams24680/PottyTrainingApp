package com.cjwilliams.pottytraining.data.auth

/**
 * What the auth interceptors need from a session: the token to send, and a way to renew it.
 * Keeps them independent of where tokens are actually stored.
 */
interface TokenRefresher {
    suspend fun accessToken(): String?

    /**
     * Renews the session and returns the new access token, or null if it could not be renewed.
     * [usedAccessToken] is the token the caller just tried, which lets concurrent callers
     * share a single refresh instead of each spending the refresh token.
     */
    suspend fun refresh(usedAccessToken: String?): String?
}
