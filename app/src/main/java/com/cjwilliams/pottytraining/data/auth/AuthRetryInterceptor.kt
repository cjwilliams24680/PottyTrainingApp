package com.cjwilliams.pottytraining.data.auth

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.cjwilliams.pottytraining.data.remote.isUnauthenticated
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Renews an expired access token and replays the operation once.
 *
 * This has to live at the GraphQL layer rather than in an HttpInterceptor: the server reports
 * a bad token as HTTP 200 with an UNAUTHENTICATED error, so there is no status code to react
 * to. The replay goes back through the whole chain, which is what lets [AuthInterceptor]
 * pick up the token this refresh just stored.
 */
class AuthRetryInterceptor(
    private val tokenRefresher: TokenRefresher
) : ApolloInterceptor {

    override fun <D : Operation.Data> intercept(
        request: ApolloRequest<D>,
        chain: ApolloInterceptorChain
    ): Flow<ApolloResponse<D>> = flow {
        // Captured before the call so a concurrent refresh can be told apart from a stale token.
        val usedAccessToken = tokenRefresher.accessToken()
        val response = chain.proceed(request).first()

        if (!response.isUnauthenticated()) {
            emit(response)
            return@flow
        }

        val refreshedToken = tokenRefresher.refresh(usedAccessToken)
        if (refreshedToken == null) {
            emit(response)
            return@flow
        }

        Timber.d("Access token renewed, retrying %s", request.operation.name())
        emitAll(chain.proceed(request))
    }
}
