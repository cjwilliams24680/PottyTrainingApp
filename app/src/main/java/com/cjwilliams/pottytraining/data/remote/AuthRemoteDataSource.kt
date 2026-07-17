package com.cjwilliams.pottytraining.data.remote

import com.apollographql.apollo.ApolloClient
import com.cjwilliams.pottytraining.data.auth.Tokens
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.User
import com.cjwilliams.pottytraining.graphql.LoginMutation
import com.cjwilliams.pottytraining.graphql.LogoutMutation
import com.cjwilliams.pottytraining.graphql.MeQuery
import com.cjwilliams.pottytraining.graphql.SignupMutation
import com.cjwilliams.pottytraining.graphql.fragment.AuthPayloadFields
import com.cjwilliams.pottytraining.graphql.fragment.UserFields
import com.cjwilliams.pottytraining.graphql.type.LoginInput
import com.cjwilliams.pottytraining.graphql.type.SignupInput
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place that speaks GraphQL for authentication. Generated types stop here.
 */
@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun login(email: String, password: String): AppResult<Tokens> =
        apolloClient.mutation(LoginMutation(LoginInput(email = email, password = password)))
            .execute()
            .toAppResult { it.login.authPayloadFields.toTokens() }

    suspend fun signup(email: String, password: String): AppResult<Tokens> =
        apolloClient.mutation(SignupMutation(SignupInput(email = email, password = password)))
            .execute()
            .toAppResult { it.signup.authPayloadFields.toTokens() }

    suspend fun logout(): AppResult<Unit> =
        apolloClient.mutation(LogoutMutation())
            .execute()
            .toAppResult { }

    suspend fun me(): AppResult<User> =
        apolloClient.query(MeQuery())
            .execute()
            .toAppResult { it.me.userFields.toDomain() }

    private fun AuthPayloadFields.toTokens() = Tokens(
        accessToken = accessToken,
        refreshToken = refreshToken
    )

    private fun UserFields.toDomain() = User(
        id = id,
        email = email,
        emailVerified = emailVerified
    )
}
