package com.cjwilliams.pottytraining.data.remote

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.testing.QueueTestNetworkTransport
import com.apollographql.apollo.testing.enqueueTestNetworkError
import com.apollographql.apollo.testing.enqueueTestResponse
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyType
import com.cjwilliams.pottytraining.graphql.CreateLogMutation
import com.cjwilliams.pottytraining.graphql.GetLogsQuery
import com.cjwilliams.pottytraining.graphql.fragment.PottyLogFields
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import com.cjwilliams.pottytraining.graphql.type.PottyType as ApolloPottyType

class PottyRemoteDataSourceTest {

    private val timestamp = Instant.parse("2026-07-16T09:30:00Z")

    private val apolloClient = ApolloClient.Builder()
        .networkTransport(QueueTestNetworkTransport())
        .build()

    private val dataSource = PottyRemoteDataSource(apolloClient)

    @After
    fun tearDown() = apolloClient.close()

    private fun fields(id: String, type: ApolloPottyType = ApolloPottyType.PEE) = PottyLogFields(
        id = id,
        type = type,
        timestamp = timestamp,
        isAccident = false,
        note = null
    )

    private fun error(message: String, code: String) = Error.Builder(message)
        .putExtension("code", code)
        .build()

    @Test
    fun `getLogs maps the response to domain logs`() = runTest {
        apolloClient.enqueueTestResponse(
            GetLogsQuery(),
            GetLogsQuery.Data(listOf(GetLogsQuery.GetLog("PottyLog", fields("log-1"))))
        )

        val result = dataSource.getLogs()

        assertEquals(
            AppResult.Success(
                listOf(PottyLog(id = "log-1", timestamp = timestamp, type = PottyType.PEE))
            ),
            result
        )
    }

    @Test
    fun `getLogs reports a transport failure as a network error`() = runTest {
        apolloClient.enqueueTestNetworkError()

        assertEquals(AppResult.Error(AppError.Network), dataSource.getLogs())
    }

    @Test
    fun `an unauthenticated error becomes an auth error`() = runTest {
        apolloClient.enqueueTestResponse(
            GetLogsQuery(),
            null,
            listOf(error("Not signed in", "UNAUTHENTICATED"))
        )

        assertEquals(AppResult.Error(AppError.Auth), dataSource.getLogs())
    }

    @Test
    fun `any other GraphQL error keeps its message`() = runTest {
        apolloClient.enqueueTestResponse(
            GetLogsQuery(),
            null,
            listOf(error("Boom", "INTERNAL_SERVER_ERROR"))
        )

        assertEquals(AppResult.Error(AppError.Server("Boom")), dataSource.getLogs())
    }

    @Test
    fun `createLog returns the log the server echoes back`() = runTest {
        val log = PottyLog(timestamp = timestamp, type = PottyType.PEE)
        apolloClient.enqueueTestResponse(
            CreateLogMutation(log.toCreateInput()),
            CreateLogMutation.Data(CreateLogMutation.CreateLog("PottyLog", fields("server-id")))
        )

        val result = dataSource.createLog(log)

        assertEquals(AppResult.Success(log.copy(id = "server-id")), result)
    }

    @Test
    fun `createLog fails when the server echoes a type this build cannot represent`() = runTest {
        val log = PottyLog(timestamp = timestamp, type = PottyType.PEE)
        apolloClient.enqueueTestResponse(
            CreateLogMutation(log.toCreateInput()),
            CreateLogMutation.Data(
                CreateLogMutation.CreateLog(
                    "PottyLog",
                    fields("server-id", ApolloPottyType.safeValueOf("VOMIT"))
                )
            )
        )

        assertEquals(
            AppResult.Error(AppError.Server("Unsupported potty type 'VOMIT'")),
            dataSource.createLog(log)
        )
    }
}
