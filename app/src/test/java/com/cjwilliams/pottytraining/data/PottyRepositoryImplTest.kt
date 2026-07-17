package com.cjwilliams.pottytraining.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.testing.MapTestNetworkTransport
import com.apollographql.apollo.testing.registerTestNetworkError
import com.apollographql.apollo.testing.registerTestResponse
import com.cjwilliams.pottytraining.data.remote.PottyRemoteDataSource
import com.cjwilliams.pottytraining.data.remote.toCreateInput
import com.cjwilliams.pottytraining.data.remote.toUpdateInput
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyType
import com.cjwilliams.pottytraining.graphql.CreateLogMutation
import com.cjwilliams.pottytraining.graphql.GetLogsQuery
import com.cjwilliams.pottytraining.graphql.UpdateLogMutation
import com.cjwilliams.pottytraining.graphql.fragment.PottyLogFields
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import com.cjwilliams.pottytraining.graphql.type.PottyType as ApolloPottyType

/**
 * Drives the repository through a real [PottyRemoteDataSource] over a fake network, so the
 * GraphQL wiring is covered too. Registering a single operation is what proves routing:
 * sending the wrong mutation finds nothing registered and fails the assertion.
 */
class PottyRepositoryImplTest {

    private val timestamp = Instant.parse("2026-07-16T09:30:00Z")

    private val apolloClient = ApolloClient.Builder()
        .networkTransport(MapTestNetworkTransport())
        .build()

    private val dao = FakePottyDao()
    private val repository = PottyRepositoryImpl(dao, PottyRemoteDataSource(apolloClient))

    @After
    fun tearDown() = apolloClient.close()

    private fun fields(id: String, note: String? = null) = PottyLogFields(
        id = id,
        type = ApolloPottyType.PEE,
        timestamp = timestamp,
        isAccident = false,
        note = note
    )

    private fun entity(id: String) = PottyEntity(
        id = id,
        timestamp = timestamp.toEpochMilli(),
        note = null,
        isAccident = false,
        type = PottyType.PEE.name
    )

    private fun registerGetLogs(vararg ids: String) {
        apolloClient.registerTestResponse(
            GetLogsQuery(),
            GetLogsQuery.Data(ids.map { GetLogsQuery.GetLog("PottyLog", fields(it)) })
        )
    }

    @Test
    fun `refreshLogs replaces the cache with what the server returned`() = runTest {
        dao.upsertLog(entity("stale"))
        registerGetLogs("fresh-1", "fresh-2")

        val result = repository.refreshLogs()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(setOf("fresh-1", "fresh-2"), dao.current.map { it.id }.toSet())
    }

    @Test
    fun `a failed refresh leaves the cached logs alone`() = runTest {
        dao.upsertLog(entity("cached"))
        apolloClient.registerTestNetworkError(GetLogsQuery())

        val result = repository.refreshLogs()

        assertEquals(AppResult.Error(AppError.Network), result)
        assertEquals(listOf("cached"), dao.current.map { it.id })
    }

    @Test
    fun `saveLog creates when the log has no id and caches the server copy`() = runTest {
        val log = PottyLog(timestamp = timestamp, type = PottyType.PEE)
        apolloClient.registerTestResponse(
            CreateLogMutation(log.toCreateInput()),
            CreateLogMutation.Data(CreateLogMutation.CreateLog("PottyLog", fields("server-id")))
        )

        val result = repository.saveLog(log)

        assertEquals(AppResult.Success(log.copy(id = "server-id")), result)
        // The id only exists server-side until now, so this proves the response was cached.
        assertEquals(listOf("server-id"), dao.current.map { it.id })
    }

    @Test
    fun `saveLog updates when the log already has an id`() = runTest {
        dao.upsertLog(entity("log-1"))
        val log = PottyLog(id = "log-1", timestamp = timestamp, note = "edited")
        apolloClient.registerTestResponse(
            UpdateLogMutation(log.toUpdateInput("log-1")),
            UpdateLogMutation.Data(
                UpdateLogMutation.UpdateLog("PottyLog", fields("log-1", note = "edited"))
            )
        )

        val result = repository.saveLog(log)

        assertEquals(AppResult.Success(log), result)
        assertEquals(listOf("edited"), dao.current.map { it.note })
    }

    @Test
    fun `a failed save leaves the cache untouched`() = runTest {
        val log = PottyLog(timestamp = timestamp, type = PottyType.PEE)
        apolloClient.registerTestNetworkError(CreateLogMutation(log.toCreateInput()))

        val result = repository.saveLog(log)

        assertEquals(AppResult.Error(AppError.Network), result)
        assertEquals(emptyList<PottyEntity>(), dao.current)
    }

    @Test
    fun `deleteLog reports that the server cannot do it yet and keeps the log`() = runTest {
        dao.upsertLog(entity("log-1"))

        val result = repository.deleteLog(PottyLog(id = "log-1", timestamp = timestamp))

        assertEquals(AppResult.Error(AppError.NotSupported), result)
        // Dropping the cached row would only bring the log back on the next refresh.
        assertEquals(listOf("log-1"), dao.current.map { it.id })
    }

    @Test
    fun `getLogs reads the cache newest first`() = runTest {
        dao.upsertLog(entity("older").copy(timestamp = 1_000))
        dao.upsertLog(entity("newer").copy(timestamp = 2_000))

        assertEquals(listOf("newer", "older"), repository.getLogs().first().map { it.id })
    }

    @Test
    fun `getLogById maps the cached row to a domain log`() = runTest {
        dao.upsertLog(entity("log-1"))

        assertEquals(
            PottyLog(id = "log-1", timestamp = timestamp, type = PottyType.PEE),
            repository.getLogById("log-1").first()
        )
    }
}
