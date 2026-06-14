package com.example.services

import com.example.models.GameRequest
import com.example.models.GameResponse
import com.example.models.Round
import com.example.models.RoundRequest
import com.example.models.Tournament
import com.example.repositories.RoundRepository
import com.example.repositories.TournamentRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RoundServiceTest {
    private val roundRepo = mockk<RoundRepository>()
    private val tournamentRepo = mockk<TournamentRepository>()
    private val gameService = mockk<GameService>()
    private val lichessService = mockk<LichessService>()
    private val service = RoundService(roundRepo, tournamentRepo, gameService, lichessService)

    private fun fakeGameRequest(tableNumber: Int = 1, white: String = "Test1", black: String = "Test2") =
        GameRequest(tableNumber = tableNumber, white = white, black = black)
    private fun fakeRoundRequest(games: List<GameRequest> = listOf(fakeGameRequest())) =
        RoundRequest(name = "Round 1", roundNumber = 1, startDate = null, games = games)
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

    private fun fakeRound() = Round(
        roundId = "r1",
        tournamentId = "t1",
        name = "Round 1",
        roundNumber = 1,
        pgn = "",
        lichessRoundId = "broadcast_round",
        status = "ACTIVE",
        startDate = null
    )

    private fun fakeGameResponse() = GameResponse(
        id = UUID.randomUUID().toString(),
        tableNumber = 1,
        pgn = "*",
        result = "*",
        white = "Test1",
        black = "Test2"
    )

    @Test
    fun create_exception_if_round_number_exists() = runBlocking {
        coEvery { roundRepo.existsByTournamentAndNumber("t1",1) } returns true
        val ex = assertFailsWith<IllegalArgumentException> {
            service.createRound("t1", fakeRoundRequest())
        }
        assertTrue(ex.message!!.contains("already exists"))
    }

    @Test
    fun create_exception_if_tournament_not_exists(): Unit = runBlocking {
        coEvery { roundRepo.existsByTournamentAndNumber("t1", 1) } returns false
        coEvery { tournamentRepo.findById("t1") } returns null
        assertFailsWith<IllegalArgumentException> {
            service.createRound("t1", fakeRoundRequest())
        }
    }

    @Test
    fun create_round_calls_lichess_with_broadcastId() = runBlocking {
        val tournament = fakeTournament()
        coEvery { roundRepo.existsByTournamentAndNumber("t1", 1) } returns false
        coEvery { tournamentRepo.findById("t1") } returns tournament
        coEvery { lichessService.createRound("broadcast_test", any()) } returns "broadcast_round"
        coEvery { gameService.createGame(any(),any(),any())} returns fakeGameResponse()
        coEvery { roundRepo.createRound(any()) } returns fakeRound()
        coEvery { roundRepo.updatePGN(any(), any()) } returns true
        coEvery { lichessService.pushRoundPgn(any(), any()) } returns "Ok"

        service.createRound("t1", fakeRoundRequest())
        coVerify{ lichessService.createRound("broadcast_test", "Round 1") }
    }

    @Test
    fun create_round_creates_games() = runBlocking {
        val tournament = fakeTournament()
        val request = fakeRoundRequest(
            games = mutableListOf(
                fakeGameRequest(1,"Test1", "Test2"),
                fakeGameRequest(2,"Test3", "Test4")
            )
        )
        coEvery { roundRepo.existsByTournamentAndNumber("t1", 1) } returns false
        coEvery { tournamentRepo.findById("t1") } returns tournament
        coEvery { lichessService.createRound("broadcast_test", any()) } returns "broadcast_round"
        coEvery { gameService.createGame(any(),any(),any())} returns fakeGameResponse()
        coEvery { roundRepo.createRound(any()) } returns fakeRound()
        coEvery { roundRepo.updatePGN(any(), any()) } returns true
        coEvery { lichessService.pushRoundPgn(any(), any()) } returns "Ok"

        service.createRound("t1", request)
        coVerify(exactly = 2) { gameService.createGame(any(),any(), any()) }
    }

    @Test
    fun create_assign_NOT_STARTED_to_round(): Unit = runBlocking {
        val tournament = fakeTournament()
        coEvery { roundRepo.existsByTournamentAndNumber("t1", 1) } returns false
        coEvery { tournamentRepo.findById("t1") } returns tournament
        coEvery { lichessService.createRound("broadcast_test", any()) } returns "broadcast_round"
        coEvery { gameService.createGame(any(),any(),any())} returns fakeGameResponse()
        coEvery { roundRepo.createRound(any()) } answers {
            val round = firstArg<Round>()
            assertEquals("NOT_STARTED", round.status)
            round
        }
        coEvery { roundRepo.updatePGN(any(), any()) } returns true
        coEvery { lichessService.pushRoundPgn(any(), any()) } returns "Ok"

        service.createRound("t1", fakeRoundRequest())
    }

    @Test
    fun getRoundByTournamentId_returns_empty_list_if_not_exists_rounds() = runBlocking {
        coEvery { roundRepo.findByTournamentId("t1") } returns emptyList()
        val result = service.getRoundsByTournamentId("t1")
        assertTrue(result.isEmpty())
    }

    @Test
    fun getRoundByTournamentId_returns_list_of_rounds() = runBlocking {
        val round1 = fakeRound().copy(roundId = "r1")
        val round2 = fakeRound().copy(roundId = "r2")
        coEvery { roundRepo.findByTournamentId("t1") } returns listOf(round1, round2)
        coEvery { gameService.getGamesByRoundId("r1") } returns listOf(fakeGameResponse())
        coEvery { gameService.getGamesByRoundId("r2") } returns listOf(fakeGameResponse(), fakeGameResponse())
        val result = service.getRoundsByTournamentId("t1")
        assertEquals(2, result.size)
        assertEquals(1, result[0].games.size)
        assertEquals(2, result[1].games.size)
    }

    @Test
    fun getRoundById_returns_null_if_not_exists() = runBlocking {
        coEvery { roundRepo.findById("r1") } returns null
        val result = service.getRoundById("r1")
        assertEquals(null, result)
    }

    @Test
    fun getRoundById_returns_round_with_games() = runBlocking {
        val round = fakeRound().copy(roundId = "r1")
        coEvery { roundRepo.findById("r1") } returns round
        coEvery { gameService.getGamesByRoundId("r1") } returns listOf(fakeGameResponse(), fakeGameResponse())
        val result = service.getRoundById("r1")
        assertEquals("r1", result?.roundId)
        assertEquals(2, result?.games?.size)
    }

    @Test
    fun updatePGN_returns_null_if_not_exists() = runBlocking {
        coEvery { roundRepo.findById("r1") } returns null
        assertNull(service.updatePGN("r1", "*"))
    }

    @Test
    fun updatePGN_calls_lichess_if_correct_update() = runBlocking {
        val round = fakeRound()
        coEvery { roundRepo.findById("r1") } returns round
        coEvery { roundRepo.updatePGN("r1", "*") } returns true
        coEvery { lichessService.pushRoundPgn(round.lichessRoundId, any()) } returns "Ok"
        service.updatePGN("r1", "*")
        coVerify { lichessService.pushRoundPgn(round.lichessRoundId, "*") }
    }

    @Test
    fun updatePGN_does_not_call_lichess_if_update_fails() = runBlocking {
        coEvery { roundRepo.findById("r1") } returns fakeRound()
        coEvery { roundRepo.updatePGN("r1", "*") } returns false
        service.updatePGN("r1", "*")
        coVerify(exactly = 0) { lichessService.pushRoundPgn(any(), any()) }
    }
}