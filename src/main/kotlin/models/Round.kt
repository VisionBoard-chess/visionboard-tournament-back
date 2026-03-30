// kotlin/models/Round.kt
package com.example.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

data class Round(
    val roundId: String,
    val tournamentId: String,
    val name: String,
    val roundNumber: Int,
    val pgn: String = "",
    val lichessRoundId: String = "",
    val status: String = "NOT_STARTED",
    val startDate: LocalDateTime? = null
)

@Serializable
data class RoundRequest(
    val name: String,
    val roundNumber: Int,
    val startDate: String? = null,
    val games: List<GameRequest> = emptyList()
)


@Serializable
data class RoundResponse(
    val roundId: String,
    val tournamentId: String,
    val name: String,
    val roundNumber: Int,
    val status: String,
    val startDate: String?,
    val games: List<GameResponse> = emptyList()
)


