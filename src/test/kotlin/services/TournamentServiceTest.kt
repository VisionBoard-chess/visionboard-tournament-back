package com.example.services

import com.example.models.Tournament
import com.example.models.TournamentPublicResponse
import com.example.models.TournamentRequest
import com.example.repositories.TournamentRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TournamentServiceTest {
    private val repo = mockk<TournamentRepository>()
    private val lichessService = mockk<LichessService>()
    private val service = TournamentService(repo, lichessService)

    private fun fakeTournament() = Tournament(
        tournamentId = "t1",
        name = "Torneo test",
        description = "Description test",
        typeOf = "closed",
        startDate = LocalDateTime.of(2025, 1, 1, 10, 0),
        creatorId = 1,
        accessCode = "ABC123",
        lichessBroadcastId = "broadcast_test",
        roundIds = emptyList(),
        status = "NOT_STARTED"
    )

    @Test
    fun create_generates_6_digit_access_code() = runBlocking {
        coEvery { lichessService.createBroadcast(any())} returns "broadcast_test"
        coEvery { repo.createTournament(any())} returns fakeTournament()
        val request = TournamentRequest(
            name = "Torneo test",
            description = "Description test",
            typeOf = "closed",
            startDate = "2025-01-01T10:00:00",
            creatorId = 1
        )
        val result = service.create(request)
        assertEquals(6, result.accessCode.length)
    }

    @Test
    fun create_assign_status_NOT_STARTED() = runBlocking {
        coEvery { lichessService.createBroadcast(any())} returns "broadcast_test"
        coEvery { repo.createTournament(any())} returns fakeTournament()
        val request = TournamentRequest(
            name = "Torneo test",
            description = "Description test",
            typeOf = "closed",
            startDate = "2025-01-01T10:00:00",
            creatorId = 1
        )
        val result = service.create(request)
        assertEquals("NOT_STARTED", result.status)
    }

    @Test
    fun create_calls_lichess_with_tournament_name() = runBlocking {
        coEvery { lichessService.createBroadcast("Torneo test")} returns "broadcast_test"
        coEvery { repo.createTournament(any())} returns fakeTournament()
        val request = TournamentRequest(
            name = "Torneo test",
            description = "Description test",
            typeOf = "closed",
            startDate = "2025-01-01T10:00:00",
            creatorId = 1
        )
        service.create(request)
        coVerify{ lichessService.createBroadcast("Torneo test") }
    }

    @Test
    fun create_generates_unique_tournamentId() = runBlocking {
        coEvery { lichessService.createBroadcast(any())} returns "broadcast_test"
        coEvery { repo.createTournament(any())} returns fakeTournament()
        val request = TournamentRequest(
            name = "Torneo test",
            description = "Description test",
            typeOf = "closed",
            startDate = "2025-01-01T10:00:00",
            creatorId = 1
        )
        val id1 = service.create(request).tournamentId
        val id2 = service.create(request).tournamentId
        assertNotEquals(id1, id2)
    }

    @Test
    fun getById_returnsNull_ifNotExists() = runBlocking {
        coEvery { repo.findById("none") } returns null
        val result = service.getById("none")
        assertNull(result)
    }

    @Test
    fun getById_returns_TournamentPublicResponse():Unit = runBlocking {
        val tournament = fakeTournament()
        coEvery { repo.findById("1") } returns tournament
        val result = service.getById("1")
        assertNotNull(result)
        assertIs<TournamentPublicResponse>(result)
    }

    @Test
    fun deleteById_returnsTrue_deleteIfExists() = runBlocking {
        coEvery { repo.deleteTournament("1") } returns true
        assertTrue(service.delete("1"))
    }

    @Test
    fun deleteById_returnsFalse_ifNotExists() = runBlocking {
        coEvery { repo.deleteTournament("none") } returns false
        assertFalse(service.delete("none"))
    }
}