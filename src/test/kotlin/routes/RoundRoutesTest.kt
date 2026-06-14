package com.example.routes

import com.example.helpers.testModule
import com.example.models.GameRequest
import com.example.models.RoundRequest
import com.example.models.RoundResponse
import com.example.models.TournamentPublicResponse
import com.example.services.RoundService
import com.example.services.TournamentService
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class RoundRoutesTest {
    private val roundService = mockk<RoundService>()
    private val tournamentService = mockk<TournamentService>()

    private fun ApplicationTestBuilder.setupApp(){
        application { testModule(tournamentService = tournamentService, roundService = roundService) }
    }

    private fun fakeRoundRequest() =
        RoundRequest(name = "Round 1", roundNumber = 1, startDate = null, games = emptyList())

    private fun fakeRoundResponse(roundNumber: Int = 1) = RoundResponse(
        roundId = "r1",
        tournamentId = "t1",
        name = "Round 1",
        roundNumber = roundNumber,
        status = "NOT_STARTED",
        startDate = null,
        games = emptyList()
    )

    private fun fakeTournamentPublicResponse() = TournamentPublicResponse(
        tournamentId = "t1",
        name = "Torneo test",
        description = "Description test",
        typeOf = "closed",
        startDate = "2025-01-01T10:00:00",
        roundIds = emptyList(),
        status = "NOT_STARTED"
    )

    @Test
    fun post_rounds_without_token_returns401() = testApplication {
        setupApp()
        val response = client.post("/tournaments/t1/rounds")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun post_rounds_with_valid_token_returns404() = testApplication {
        setupApp()
        coEvery { tournamentService.getById("t1") } returns null
        val response = client.post("/tournaments/t1/rounds") {
            header("Authorization", "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(fakeRoundRequest()))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun post_rounds_with_valid_token_returns201() = testApplication {
        setupApp()
        coEvery { tournamentService.getById("t1") } returns fakeTournamentPublicResponse()
        coEvery { roundService.createRound("t1", any()) } returns fakeRoundResponse()
        val response = client.post("/tournaments/t1/rounds") {
            header("Authorization", "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(fakeRoundRequest()))
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun post_rounds_with_valid_token_returns409() = testApplication {
        setupApp()
        coEvery { tournamentService.getById("t1") } returns fakeTournamentPublicResponse()
        coEvery { roundService.createRound("t1", any()) } throws
                IllegalArgumentException("Round number 1 already exists")
        val response = client.post("/tournaments/t1/rounds") {
            header("Authorization", "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(fakeRoundRequest()))
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun get_rounds_returns200() = testApplication {
        setupApp()
        coEvery { roundService.getRoundsByTournamentId("t1")} returns listOf(fakeRoundResponse())
        val response = client.get("/tournaments/t1/rounds") {
            header("Authorization", "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun get_next_round_number_returns_number_1() = testApplication {
        setupApp()
        coEvery { roundService.getRoundsByTournamentId("t1") } returns emptyList()
        val response = client.get("/tournaments/t1/rounds/next-round-number") {
            header("Authorization", "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("1"))
    }

    @Test
    fun get_next_round_number_returns_number_3() = testApplication {
        setupApp()
        coEvery { roundService.getRoundsByTournamentId("t1") } returns listOf(
            fakeRoundResponse(roundNumber = 1),
            fakeRoundResponse(roundNumber = 2),
            fakeRoundResponse(roundNumber = 4)
            )
        val response = client.get("/tournaments/t1/rounds/next-round-number") {
            header("Authorization", "Bearer valid_token")
        }
        assertTrue(response.bodyAsText().contains("3"))
    }

    @Test
    fun get_round_by_id_returns200() = testApplication {
        setupApp()
        coEvery { roundService.getRoundById("r1") } returns fakeRoundResponse()
        val response = client.get("tournaments/t1/rounds/r1") {
            header("Authorization", "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun get_round_by_id_returns404() = testApplication {
        setupApp()
        coEvery { roundService.getRoundById("r1") } returns null
        val response = client.get("/tournaments/t1/rounds/r1") {
            header("Authorization", "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun put_status_returns409() = testApplication {
        setupApp()
        coEvery { roundService.hasAnotherActiveRound("t1", "r1") } returns true
        val response = client.put("/tournaments/t1/rounds/r1/status") {
            header("Authorization", "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"status": "ACTIVE"}""")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun put_status_returns200() = testApplication {
        setupApp()
        coEvery { roundService.hasAnotherActiveRound("t1", "r1") } returns false
        coEvery { roundService.updateStatus("r1", "ACTIVE") } returns true
        val response = client.put("/tournaments/t1/rounds/r1/status") {
            header("Authorization", "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"status": "ACTIVE"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}