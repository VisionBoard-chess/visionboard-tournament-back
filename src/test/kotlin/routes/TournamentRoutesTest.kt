package com.example.routes

import com.example.helpers.testModule
import com.example.models.Tournament
import com.example.models.TournamentPublicResponse
import com.example.models.TournamentRequest
import com.example.models.TournamentResponse
import com.example.services.TournamentService
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
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

class TournamentRoutesTest {
    private val service = mockk<TournamentService>()
    private fun ApplicationTestBuilder.setupApp(){
        application {testModule(tournamentService = service)}
    }

    private fun fakeTournamentResponse() = TournamentResponse(
        tournamentId = "t1",
        name = "Torneo test",
        description = "Description test",
        typeOf = "closed",
        startDate = "2025-01-01T10:00:00",
        creatorId = 1,
        accessCode = "ABC123",
        lichessBroadcastId = "broadcast_test",
        roundIds = emptyList(),
        status = "NOT_STARTED"
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
    fun post_tournament_without_token_returns401() = testApplication {
        setupApp()
        val response = client.post("/tournaments")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun post_tournament_with_token_returns201() = testApplication {
        setupApp()
        val request = TournamentRequest(
            name = "Torneo test",
            description = "Description test",
            typeOf = "closed",
            startDate = "2025-01-01T10:00:00",
            creatorId = 1
        )
        coEvery { service.create(request) } returns fakeTournamentResponse()
        val response = client.post("/tournaments"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun get_tournaments_returns200() = testApplication {
        setupApp()
        coEvery { service.getAll() } returns listOf()
        val response = client.get("/tournaments"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun get_tournaments_return401() = testApplication {
        setupApp()
        val response = client.get("/tournaments")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun get_tournament_by_id_returns200() = testApplication {
        setupApp()
        coEvery { service.getById("t1") } returns fakeTournamentPublicResponse()
        val response = client.get("/tournaments/t1"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun get_tournament_by_id_returns404() = testApplication {
        setupApp()
        coEvery { service.getById("t1") } returns null
        val response = client.get("/tournaments/t1"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun delete_tournament_by_id_returns204() = testApplication {
        setupApp()
        coEvery { service.delete("t1") } returns true
        val response = client.delete("/tournaments/t1"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun delete_tournament_by_id_returns404() = testApplication {
        setupApp()
        coEvery { service.delete("none") } returns false
        val response = client.delete("/tournaments/none"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun get_tournaments_creator_returns200() = testApplication {
        setupApp()
        coEvery { service.getByCreatorId(1) } returns listOf()
        val response = client.get("/tournaments/creator/1"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}