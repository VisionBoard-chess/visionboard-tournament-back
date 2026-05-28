package com.example.models

import kotlinx.serialization.Serializable


@Serializable
data class Game(
    val id: String,
    val roundId: String,
    val tableNumber: Int,
    val whiteName: String,
    val blackName: String,
    val pgnHeader: String,
    val pgnMoves: String = "*",
    val result: String = "*"
)

@Serializable
data class GameRequest(
    val tableNumber: Int,
    val white: String,
    val black: String
)


@Serializable
data class GameResponse(
    val id: String,
    val tableNumber: Int,
    val white: String,
    val black: String,
    val pgn: String,
    val result: String
)
@Serializable
data class GameUpdateResponse(
    val id: String,
    val pgn: String,
    val roundId: String
)
@Serializable
data class IndividualGameRequest(
    val whiteName: String,
    val blackName: String,
    val pgnHeader: String,
    val pgnMoves: String = "*",
    val result: String = "*",
    val creatorId: Int,
    val participantId: Int? = null
)
@Serializable
data class IndividualGame(
    val id: String,
    val createdAt: String,
    val white: String,
    val black: String,
    val pgn: String,
    val result: String
)