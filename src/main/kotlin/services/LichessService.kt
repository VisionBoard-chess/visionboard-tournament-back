package com.example.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LichessTourResponse(val tour: LichessTour)

@Serializable
data class LichessTour(val id: String)

@Serializable
data class LichessRoundResponse(val round: LichessRound)

@Serializable
data class LichessRound(val id: String)

class LichessService(private val token: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "https://lichess.org"

    suspend fun createBroadcast(name: String): String {
        val response: LichessTourResponse = client.post("$baseUrl/broadcast/new") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
                append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            }
            setBody("name=${name.encodeURLParameter()}")
        }.body()
        return response.tour.id
    }

    suspend fun createRound(broadcastId: String, roundName: String): String {
        val response: LichessRoundResponse = client.post("$baseUrl/broadcast/$broadcastId/new") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
                append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            }
            setBody("name=${roundName.encodeURLParameter()}")
        }.body()
        return response.round.id
    }

    suspend fun pushRoundPgn(roundId: String, pgnContent: String): String {
        return client.post("$baseUrl/api/broadcast/round/$roundId/push") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
                append(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            }
            setBody(pgnContent)
        }.bodyAsText()
    }
}
