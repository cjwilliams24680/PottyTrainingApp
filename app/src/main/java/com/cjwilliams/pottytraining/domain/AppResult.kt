package com.cjwilliams.pottytraining.domain

sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    /** The request never reached the server, or the response could not be parsed. */
    data object Network : AppError

    /** The server rejected the request because the caller isn't authenticated. */
    data object Auth : AppError

    /** The operation isn't supported by the server yet. */
    data object NotSupported : AppError

    /** The server returned a GraphQL error. */
    data class Server(val message: String?) : AppError
}
