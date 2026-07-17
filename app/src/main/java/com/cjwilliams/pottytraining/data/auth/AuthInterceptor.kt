package com.cjwilliams.pottytraining.data.auth

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the current access token to every request.
 *
 * The token is read per request rather than captured once, so a retry issued by
 * [AuthRetryInterceptor] after a refresh automatically picks up the new one.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenRefresher: TokenRefresher
) : HttpInterceptor {

    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        val accessToken = tokenRefresher.accessToken() ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder().addHeader("Authorization", "Bearer $accessToken").build()
        )
    }
}
