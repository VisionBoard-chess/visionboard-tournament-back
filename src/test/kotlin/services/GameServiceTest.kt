package com.example.services

import com.example.client.GrpcGameClient
import com.example.models.Game
import com.example.models.GameRequest
import com.example.models.GameResponse
import com.example.models.IndividualGame
import com.example.models.IndividualGameRequest
import com.example.repositories.GameRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class GameServiceTest {
    private val gameRepo = mockk<GameRepository>()
    private val grpcClient = mockk<GrpcGameClient>()
    private val service = GameService(gameRepo, grpcClient)

    private fun fakeResponse() = GameResponse(
        id = "g1",
        tableNumber = 1,
        white = "Test1",
        black = "Test2",
        pgn = "*",
        result = "*"
    )

    @Test
    fun createGame_creates_pgn_headers() = runBlocking {
        val response = fakeResponse()
        coEvery { gameRepo.createTournamentGame(any()) } returns response
        val result = service.createGame("r1", GameRequest(1,"Test1", "Test2"), "[Event]")
        assertEquals("g1", result.id)
        assertEquals("Test1", result.white)
        assertEquals("Test2", result.black)
    }

    @Test
    fun createGame_unique_id() = runBlocking {
        coEvery { gameRepo.createTournamentGame(any()) } answers {
            val game = firstArg<Game>()
            GameResponse(id = game.id, game.tableNumber, game.whiteName, game.blackName, game.pgnHeader, game.result)
        }
        val request = GameRequest(1, "Test1", "Test2")
        val id1 = service.createGame("r1", request, "[Event]").id
        val id2 = service.createGame("r1", request, "[Event]").id
        assertNotEquals(id1, id2)
    }

    @Test
    fun editMove_calls_grpcClient() = runBlocking {
        coEvery { grpcClient.editMove("g1", 2, "Nf3") } returns true
        val result = service.editMove("g1", 2, "Nf3")
        assertTrue(result)
        coVerify { grpcClient.editMove("g1", 2, "Nf3") }
    }

    @Test
    fun editMove_returns_false_grpcClient_doesNot_accept_move() = runBlocking {
        coEvery { grpcClient.editMove("g1", 2, "Nf3") } returns false
        assertFalse(service.editMove("g1", 2, "Nf3"))
    }

    @Test
    fun editMove_exception_grpcClient():Unit = runBlocking {
        coEvery { grpcClient.editMove(any(), any(), any()) } throws RuntimeException("gRPC unavailable")
        assertFailsWith<RuntimeException> {
            service.editMove("g1", 2, "Nf3")
        }
    }

    @Test
    fun addMove_calls_grpcClient() = runBlocking {
        coEvery { grpcClient.addMove("g1", "Nf3") } returns true
        val result = service.addMove("g1",  "Nf3")
        assertTrue(result)
        coVerify { grpcClient.addMove("g1", "Nf3") }
    }

    @Test
    fun addMove_returns_false_grpcClient_doesNot_accept_move() = runBlocking {
        coEvery { grpcClient.addMove("g1", "Nf3") } returns false
        assertFalse(service.addMove("g1", "Nf3"))
    }

    @Test
    fun createIndividualGame_unique_id() = runBlocking {
        val request = IndividualGameRequest("Test1", "Test2", "[Event]", creatorId = 1)
        coEvery { gameRepo.createIndividualGame(any(), request) } answers {
            IndividualGame(id = firstArg(), white = request.whiteName, black = request.blackName, pgn = request.pgnHeader, createdAt = "2026-01-01", result = "*")
        }
        val id1 = service.createIndividualGame(request)
        val id2 = service.createIndividualGame(request)
        assertNotEquals(id1, id2)
    }
}