package com.cjwilliams.pottytraining.data.auth

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.testing.QueueTestNetworkTransport
import com.apollographql.apollo.testing.enqueueTestResponse
import com.cjwilliams.pottytraining.graphql.GetLogsQuery
import com.cjwilliams.pottytraining.graphql.fragment.PottyLogFields
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import com.cjwilliams.pottytraining.graphql.type.PottyType as ApolloPottyType

/**
 * The server reports a bad token as HTTP 200 plus an UNAUTHENTICATED error rather than a 401,
 * so these tests pin the retry to the GraphQL error and not to a status code.
 */
class AuthRetryInterceptorTest {

    private val transport = QueueTestNetworkTransport()
    private val tokenManager = RecordingTokenManager()

    private val apolloClient = ApolloClient.Builder()
        .networkTransport(transport)
        .addInterceptor(AuthRetryInterceptor(tokenManager))
        .build()

    @After
    fun tearDown() = apolloClient.close()

    private val log = PottyLogFields(
        id = "log-1",
        type = ApolloPottyType.PEE,
        timestamp = Instant.parse("2026-07-16T09:30:00Z"),
        isAccident = false,
        note = null
    )

    private fun successData() = GetLogsQuery.Data(listOf(GetLogsQuery.GetLog("PottyLog", log)))

    private fun enqueueUnauthenticated() {
        apolloClient.enqueueTestResponse(
            GetLogsQuery(),
            null,
            listOf(Error.Builder("Unauthorized").putExtension("code", "UNAUTHENTICATED").build())
        )
    }

    @Test
    fun `an unauthenticated response triggers a refresh and a retry`() = runTest {
        enqueueUnauthenticated()
        apolloClient.enqueueTestResponse(GetLogsQuery(), successData())

        val response = apolloClient.query(GetLogsQuery()).execute()

        assertEquals(successData(), response.data)
        assertEquals(1, tokenManager.refreshCount)
    }

    @Test
    fun `the refresh is told which token failed so a concurrent refresh is not repeated`() =
        runTest {
            tokenManager.accessToken = "stale-token"
            enqueueUnauthenticated()
            apolloClient.enqueueTestResponse(GetLogsQuery(), successData())

            apolloClient.query(GetLogsQuery()).execute()

            assertEquals("stale-token", tokenManager.refreshedWith)
        }

    @Test
    fun `a successful response is passed through without refreshing`() = runTest {
        apolloClient.enqueueTestResponse(GetLogsQuery(), successData())

        val response = apolloClient.query(GetLogsQuery()).execute()

        assertEquals(successData(), response.data)
        assertEquals(0, tokenManager.refreshCount)
    }

    @Test
    fun `a failed refresh surfaces the original error instead of retrying`() = runTest {
        tokenManager.refreshResult = null
        enqueueUnauthenticated()

        val response: ApolloResponse<GetLogsQuery.Data> =
            apolloClient.query(GetLogsQuery()).execute()

        assertNull(response.data)
        assertEquals("Unauthorized", response.errors?.single()?.message)
        assertEquals(1, tokenManager.refreshCount)
    }
}

private class RecordingTokenManager : TokenRefresher {
    var accessToken: String? = "access-token"
    var refreshResult: String? = "new-token"
    var refreshCount = 0
    var refreshedWith: String? = null

    override suspend fun accessToken(): String? = accessToken

    override suspend fun refresh(usedAccessToken: String?): String? {
        refreshCount++
        refreshedWith = usedAccessToken
        return refreshResult
    }
}
