package com.example.routes

import com.example.helpers.testModule
import com.example.models.GameResponse
import com.example.models.GameUpdateResponse
import com.example.models.IndividualGame
import com.example.models.IndividualGameRequest
import com.example.services.GameService
import com.example.services.RoundService
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class GameRoutesTest {
    private val gameService = mockk<GameService>()
    private val roundService = mockk<RoundService>()

    private fun ApplicationTestBuilder.setupApp(){
        application {testModule(gameService = gameService, roundService = roundService) }
    }

    private fun fakeGameResponse() = GameResponse(
        id = "g1",
        tableNumber = 1,
        white = "Test1",
        black = "Test2",
        pgn = "*",
        result = "*"
    )

    private fun fakeGameUpdateResponse() = GameUpdateResponse(
        id = "g1",
        pgn = "*",
        roundId = "r1"
    )

    private fun fakeIndividualGameRequest() = IndividualGameRequest(
        whiteName = "Test1",
        blackName = "Test2",
        pgnHeader = "[Event]",
        creatorId = 1
    )

    private fun fakeIndividualGameResponse() = IndividualGame(
        id = "ig1",
        createdAt = "2026-01-01T00:00:00Z",
        white = "Test1",
        black = "Test2",
        pgn = "*",
        result = "*"
    )

    @Test
    fun getTournamentGameById_returns200() = testApplication {
        setupApp()
        coEvery { gameService.getTournamentGameById("g1") } returns fakeGameResponse()
        val response = client.get("/games/g1"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun getTournamentGameById_returns404() = testApplication {
        setupApp()
        coEvery { gameService.getTournamentGameById("g1") } returns null
        val response = client.get("/games/g1"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun getGamesWithAccessCode_returns200() = testApplication {
        setupApp()
        coEvery { gameService.getGamesByAccessCode("ac") } returns listOf(fakeGameResponse())
        val response = client.get("/games/tournament/ac"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun getGamesByRoundId_returns200() = testApplication {
        setupApp()
        coEvery { gameService.getGamesByRoundId("r1") } returns listOf(fakeGameResponse())
        val response = client.get("/games/round/r1"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun putPGN_returns200() = testApplication {
        setupApp()
        coEvery { gameService.updatePGN("g1", "*") } returns true
        coEvery { gameService.getGameByIdServerResponse("g1") } returns fakeGameUpdateResponse()
        coEvery { roundService.syncRoundPgnFromGames("r1") } returns true
        val response = client.put("/games/g1/pgn"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"pgn":"*"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun putPGN_returns404_nonexistent() = testApplication {
        setupApp()
        coEvery { gameService.updatePGN("g1", "*") } returns false
        val response = client.put("/games/g1/pgn"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"pgn":"*"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun putPGN_returns400() = testApplication {
        setupApp()
        val response = client.put("/games/g1/pgn"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"movimientos":"*"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun putMoveEdit_returns200() = testApplication {
        setupApp()
        coEvery { gameService.editMove("g1", 1,"e4") } returns true
        val response = client.put("/games/g1/move/edit"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"moveIndex":"1","moveSan":"e4"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun putMoveEdit_returns400_nonAcceptableMove() = testApplication {
        setupApp()
        coEvery { gameService.editMove("g1", 1,"ppp") } returns false
        val response = client.put("/games/g1/move/edit"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"moveIndex":"1","moveSan":"ppp"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun putMoveEdit_returns400_nonAcceptableIndex() = testApplication {
        setupApp()
        val response = client.put("/games/g1/move/edit"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"moveIndex":"ppp","moveSan":"e4"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun putMoveAdd_returns200() = testApplication {
        setupApp()
        coEvery { gameService.addMove("g1","e4") } returns true
        val response = client.put("/games/g1/move/add"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"moveSan":"e4"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun putMoveAdd_returns400_nonAcceptableMove() = testApplication {
        setupApp()
        coEvery { gameService.addMove("g1", "ppp") } returns false
        val response = client.put("/games/g1/move/add"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"moveSan":"ppp"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun getGamesByUserId_returns200() = testApplication {
        setupApp()
        coEvery { gameService.getGamesByUserId(1) } returns listOf()
        val response = client.get("/games/user/1"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun postIndividualGame_returns200() = testApplication {
        setupApp()
        val request = fakeIndividualGameRequest()
        coEvery { gameService.createIndividualGame(request) } returns fakeIndividualGameResponse()
        val response = client.post("/games/individual/create") {
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun postIndividualGame_returns400() = testApplication {
        setupApp()
        val response = client.post("/games/individual/create") {
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"whiteName":"Test1","blackName":"Test2"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun putPGNIndividualGame_returns200() = testApplication {
        setupApp()
        coEvery { gameService.updatePGN("g1", "g1") } returns true
        val response = client.put("/games/g1/pgn/individual"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"pgn":"g1"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun putPGNIndividualGame_returns404() = testApplication {
        setupApp()
        coEvery { gameService.updatePGN("g1", any()) } returns false
        val response = client.put("/games/g1/pgn/individual"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"pgn":"g1"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun putResultIndividualGame_returns200() = testApplication {
        setupApp()
        coEvery { gameService.updateResult("g1", "1-0") } returns true
        val response = client.put("/games/g1/result/individual"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"result":"1-0"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun putResultIndividualGame_returns404() = testApplication {
        setupApp()
        coEvery { gameService.updateResult("g1", any()) } returns false
        val response = client.put("/games/g1/result/individual"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody("""{"result":"1/2-1/2"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}