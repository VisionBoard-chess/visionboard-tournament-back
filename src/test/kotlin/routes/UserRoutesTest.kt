package com.example.routes

import com.example.helpers.testModule
import com.example.models.UserRequest
import com.example.models.UserResponse
import com.example.services.UserService
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoutesTest {
    private val userService = mockk<UserService>()
    private fun ApplicationTestBuilder.setupApp() {
        application { testModule(userService = userService) }
    }

    @Test
    fun check_nickname_returns200_if_nickname_available() = testApplication {
        setupApp()
        coEvery { userService.nicknameExists("pepe")} returns false
        val response = client.get("/user/check-nickname?nickname=pepe")
        assertEquals(HttpStatusCode.OK, response.status)
    }
    @Test
    fun check_nickname_returns409_if_nickname_used() = testApplication {
        setupApp()
        coEvery { userService.nicknameExists("pepe")} returns true
        val response = client.get("/user/check-nickname?nickname=pepe")
        assertEquals(HttpStatusCode.Conflict, response.status)
    }
    @Test
    fun check_nickname_returns400_error_param() = testApplication {
        setupApp()
        val response = client.get("/user/check-nickname")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun register_return201_user_created() = testApplication {
        setupApp()
        val request = UserRequest(nickname = "pepe", firebaseUid = "pepe1")
        val expected = UserResponse(id = 1, nickname = "pepe")
        coEvery { userService.create(request) } returns expected
        val response = client.post("/user/register"){
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(expected, Json.decodeFromString(response.bodyAsText()))
    }
    @Test
    fun register_return400_invalid_body() = testApplication {
        setupApp()
        val response = client.post("/user/register"){
            contentType(ContentType.Application.Json)
            setBody("{invalid json}")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}