package com.cjwilliams.pottytraining.data.remote

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import timber.log.Timber

/**
 * Folds an Apollo response into an [AppResult] so callers never see generated types.
 *
 * Local failures (no connectivity, malformed JSON) arrive as [ApolloResponse.exception];
 * failures the server reports arrive as [ApolloResponse.errors]. Partial data alongside
 * errors is treated as a failure — no operation here has a useful partial result.
 */
internal fun <D : Operation.Data, T> ApolloResponse<D>.toAppResult(
    transform: (D) -> T
): AppResult<T> {
    val exception = exception
    if (exception != null) {
        Timber.w(exception, "%s failed", operation.name())
        return AppResult.Error(AppError.Network)
    }

    val errors = errors
    if (!errors.isNullOrEmpty()) {
        Timber.w("%s returned errors: %s", operation.name(), errors.joinToString { it.message })
        return AppResult.Error(errors.toAppError())
    }

    val data = data
    if (data == null) {
        Timber.w("%s returned neither data nor errors", operation.name())
        return AppResult.Error(AppError.Network)
    }

    return AppResult.Success(transform(data))
}

/**
 * True when the server rejected the request for want of a valid token.
 *
 * The server answers HTTP 200 with this extension code rather than a 401, so auth failures
 * are only visible once the GraphQL body has been parsed.
 */
internal fun ApolloResponse<*>.isUnauthenticated(): Boolean =
    errors?.any { it.isUnauthenticated() } == true

private fun List<Error>.toAppError(): AppError =
    if (any { it.isUnauthenticated() }) AppError.Auth else AppError.Server(first().message)

private fun Error.isUnauthenticated(): Boolean = extensions?.get("code") == UNAUTHENTICATED_CODE

private const val UNAUTHENTICATED_CODE = "UNAUTHENTICATED"
