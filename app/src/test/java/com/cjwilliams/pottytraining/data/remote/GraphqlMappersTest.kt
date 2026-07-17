package com.cjwilliams.pottytraining.data.remote

import com.apollographql.apollo.api.Optional
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyType
import com.cjwilliams.pottytraining.graphql.fragment.PottyLogFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import com.cjwilliams.pottytraining.graphql.type.PottyType as ApolloPottyType

class GraphqlMappersTest {

    private val timestamp = Instant.parse("2026-07-16T09:30:00Z")

    private fun fields(
        id: String = "log-1",
        type: ApolloPottyType = ApolloPottyType.PEE,
        note: String? = "on the potty"
    ) = PottyLogFields(
        id = id,
        type = type,
        timestamp = timestamp,
        isAccident = false,
        note = note
    )

    @Test
    fun `fragment maps to domain log`() {
        val log = fields().toDomain()

        assertEquals(
            PottyLog(
                id = "log-1",
                timestamp = timestamp,
                note = "on the potty",
                isAccident = false,
                type = PottyType.PEE
            ),
            log
        )
    }

    @Test
    fun `fragment keeps a null note null`() {
        assertNull(fields(note = null).toDomain()?.note)
    }

    @Test
    fun `fragment with an unknown type maps to null`() {
        val unknown = ApolloPottyType.safeValueOf("VOMIT")

        assertNull(fields(type = unknown).toDomain())
    }

    @Test
    fun `unknown types are dropped from a list rather than failing it`() {
        val logs = listOf(
            fields(id = "known"),
            fields(id = "unknown", type = ApolloPottyType.safeValueOf("VOMIT"))
        ).toDomain()

        assertEquals(listOf("known"), logs.map { it.id })
    }

    @Test
    fun `create input omits an absent note`() {
        val input = PottyLog(timestamp = timestamp, note = null).toCreateInput()

        assertEquals(Optional.Absent, input.note)
    }

    @Test
    fun `create input carries the log fields`() {
        val input = PottyLog(
            timestamp = timestamp,
            note = "a note",
            isAccident = true,
            type = PottyType.BOTH
        ).toCreateInput()

        assertEquals(Optional.Present("a note"), input.note)
        assertEquals(timestamp, input.timestamp)
        assertEquals(true, input.isAccident)
        assertEquals(ApolloPottyType.BOTH, input.type)
    }

    @Test
    fun `update input sends a null note as present so the server clears it`() {
        val input = PottyLog(id = "log-1", timestamp = timestamp, note = null).toUpdateInput("log-1")

        // Optional.Absent here would silently keep a note the user just erased.
        assertEquals(Optional.Present(null), input.note)
    }

    @Test
    fun `update input carries every field for a full snapshot`() {
        val input = PottyLog(
            id = "log-1",
            timestamp = timestamp,
            note = "a note",
            isAccident = true,
            type = PottyType.POO
        ).toUpdateInput("log-1")

        assertEquals("log-1", input.id)
        assertEquals(Optional.Present("a note"), input.note)
        assertEquals(Optional.Present(timestamp), input.timestamp)
        assertEquals(Optional.Present(true), input.isAccident)
        assertEquals(Optional.Present<ApolloPottyType?>(ApolloPottyType.POO), input.type)
    }

    @Test
    fun `potty types round trip through the generated enum`() {
        PottyType.entries.forEach { type ->
            assertEquals(type, type.toApollo().toDomain())
        }
    }
}
