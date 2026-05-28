package com.example.services

import com.example.client.GrpcGameClient
import com.example.models.Game
import com.example.models.GameRequest
import com.example.models.GameResponse
import com.example.models.GameUpdateResponse
import com.example.models.IndividualGame
import com.example.models.IndividualGameRequest
import com.example.repositories.GameRepository
import java.util.UUID

class GameService (private val gameRepo: GameRepository, private val grpcClient: GrpcGameClient) {
    suspend fun getGamesByRoundId(roundId: String): List<GameResponse> {
        return gameRepo.getGamesByRoundId(roundId)
    }

    suspend fun getGamesByAccessCode(accessCode: String): List<GameResponse> {
        return gameRepo.getGamesByAccessCode(accessCode)
    }

    suspend fun getTournamentGameById(gameId: String): GameResponse? {
        return gameRepo.getTournamentGameById(gameId)
    }

    suspend fun getGameByIdServerResponse(gameId: String): GameUpdateResponse? {
        return gameRepo.getGameByIdServerResponse(gameId)
    }

    suspend fun updatePGN(gameId: String, pgn: String): Boolean {
        return gameRepo.updatePGNMoves(gameId, pgn)
    }

    suspend fun editMove(gameId: String, moveIndex: Int, moveSan: String):Boolean {
        return grpcClient.editMove(gameId, moveIndex, moveSan)
    }

    suspend fun addMove(gameId: String, moveSan: String): Boolean {
        return grpcClient.addMove(gameId, moveSan)
    }

    suspend fun createGame(roundId: String, request: GameRequest, pgnHeader: String): GameResponse {
        val game = Game(
            id = UUID.randomUUID().toString(),
            roundId = roundId,
            tableNumber = request.tableNumber,
            whiteName = request.white,
            blackName = request.black,
            pgnHeader = pgnHeader,
            pgnMoves = "*",
            result = "*"
        )
        return gameRepo.createTournamentGame(game)
    }

    suspend fun getGamesByUserId(userId: Int): List<IndividualGame> {
        return gameRepo.getIndividualGamesByPlayer(userId)
    }

    suspend fun createIndividualGame(individualGame: IndividualGameRequest): IndividualGame {
        val id = UUID.randomUUID().toString()
        return gameRepo.createIndividualGame(id, individualGame)
    }

    suspend fun updateResult(gameId: String, result: String): Boolean{
        return gameRepo.updateResult(gameId, result)
    }
}