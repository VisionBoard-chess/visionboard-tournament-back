package com.example.routes

import com.example.helpers.testModule
import com.example.models.UserResponse
import com.example.services.UserService
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class ProtectedUserRoutesTest {
    private val userService = mockk<UserService>()
    private fun ApplicationTestBuilder.setupApp() {
        application { testModule(userService = userService) }
    }
    @Test
    fun get_users_without_token_returns401() = testApplication {
        setupApp()
        val response = client.get("/user")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
    @Test
    fun get_users_with_valid_token_returns200() = testApplication {
        setupApp()
        coEvery {userService.getAllUsers()} returns listOf()
        val response = client.get("/user"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
    @Test
    fun get_user_with_firebaseUid_returns200() = testApplication {
        setupApp()
        val expected = UserResponse(id = 1, nickname = "pepe")
        coEvery {userService.getByFirebaseUid("uid_pepe1")} returns expected
        val response = client.get("/user/uid_pepe1"){
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
    @Test
    fun delete_user_returns200() = testApplication {
        setupApp()
        coEvery { userService.deleteByFirebaseUid("uid_pepe1") } just Runs
        val response = client.delete("/user/uid_pepe1") {
            header(HttpHeaders.Authorization, "Bearer valid_token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}