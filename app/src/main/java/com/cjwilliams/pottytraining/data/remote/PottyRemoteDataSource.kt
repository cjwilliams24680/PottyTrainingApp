package com.cjwilliams.pottytraining.data.remote

import com.apollographql.apollo.ApolloClient
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.graphql.CreateLogMutation
import com.cjwilliams.pottytraining.graphql.GetLogsQuery
import com.cjwilliams.pottytraining.graphql.UpdateLogMutation
import com.cjwilliams.pottytraining.graphql.fragment.PottyLogFields
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place that speaks GraphQL for potty logs. Generated types stop here; callers get
 * domain types wrapped in [AppResult].
 */
@Singleton
class PottyRemoteDataSource @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getLogs(): AppResult<List<PottyLog>> =
        apolloClient.query(GetLogsQuery())
            .execute()
            .toAppResult { data -> data.getLogs.map { it.pottyLogFields }.toDomain() }

    suspend fun createLog(log: PottyLog): AppResult<PottyLog> =
        apolloClient.mutation(CreateLogMutation(log.toCreateInput()))
            .execute()
            .toAppResult { it.createLog.pottyLogFields }
            .requireDomainLog()

    suspend fun updateLog(id: String, log: PottyLog): AppResult<PottyLog> =
        apolloClient.mutation(UpdateLogMutation(log.toUpdateInput(id)))
            .execute()
            .toAppResult { it.updateLog.pottyLogFields }
            .requireDomainLog()

    /**
     * A mutation echoes back the log we just sent, so a type we can't map is a server bug
     * rather than a row to quietly skip the way [getLogs] does.
     */
    private fun AppResult<PottyLogFields>.requireDomainLog(): AppResult<PottyLog> = when (this) {
        is AppResult.Success -> data.toDomain()?.let { AppResult.Success(it) }
            ?: AppResult.Error(AppError.Server("Unsupported potty type '${data.type.rawValue}'"))

        is AppResult.Error -> this
    }
}
