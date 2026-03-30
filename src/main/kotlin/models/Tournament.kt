package com.example.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

data class Tournament(
    val tournamentId: String,
    val name: String,
    val description: String,
    val typeOf: String,
    val startDate: LocalDateTime,
    val creatorId: String,
    val accessCode: String,
    val lichessBroadcastId: String,
    val roundIds: List<String> = emptyList(),
    val status: String = "NOT_STARTED"
)

@Serializable
data class TournamentRequest(
    val name: String,
    val description: String = "",
    val typeOf: String,
    val startDate: String,
    val creatorId: String,
    val roundIds: List<String> = emptyList()
)

@Serializable
data class TournamentResponse(
    val tournamentId: String,
    val name: String,
    val description: String,
    val typeOf: String,
    val startDate: String,
    val creatorId: String,
    val accessCode: String,
    val lichessBroadcastId: String,
    val roundIds: List<String>,
    val status: String
)

@Serializable
data class TournamentPublicResponse(
    val tournamentId: String,
    val name: String,
    val description: String,
    val typeOf: String,
    val startDate: String,
    val roundIds: List<String>,
    val status: String
)