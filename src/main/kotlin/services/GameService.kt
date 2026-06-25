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

/**
 * GameService
 * * Domain service component managing chess match lifecycles and operational persistence.
 *
 * This layer coordinates basic database CRUD interactions alongside real-time
 * move manipulation events routed through an external engine using a high-performance
 * gRPC client infrastructure.
 *
 * Parameters
 * ----------
 * gameRepo : GameRepository
 * Data gateway orchestrating direct persistence tasks on match records.
 * grpcClient : GrpcGameClient
 * Remote procedure call client directing streaming move additions and board state edits.
 */
class GameService (private val gameRepo: GameRepository, private val grpcClient: GrpcGameClient) {

    /**
     * Resolves all tournament game instances linked to a shared round identifier.
     *
     * Parameters
     * ----------
     * roundId : String
     * Unique system identification key for the parent round.
     *
     * Results
     * -------
     * List<GameResponse>
     * A collection of presentation-ready match DTO profiles mapped to the target round.
     */
    suspend fun getGamesByRoundId(roundId: String): List<GameResponse> {
        return gameRepo.getGamesByRoundId(roundId)
    }

    /**
     * Pulls active match structures associated with a specific broadcast access token.
     *
     * Parameters
     * ----------
     * accessCode : String
     * Security authorization pairing token string.
     *
     * Results
     * -------
     * List<GameResponse>
     * Matched collection profiles authorized under the specified credential code.
     */
    suspend fun getGamesByAccessCode(accessCode: String): List<GameResponse> {
        return gameRepo.getGamesByAccessCode(accessCode)
    }

    /**
     * Resolves a discrete tournament game instance verified by its system key.
     *
     * Parameters
     * ----------
     * gameId : String
     * Unique system key of the specific match target.
     *
     * Results
     * -------
     * GameResponse?
     * The compiled match details DTO, or `null` if no match records correspond to the key.
     */
    suspend fun getTournamentGameById(gameId: String): GameResponse? {
        return gameRepo.getTournamentGameById(gameId)
    }

    /**
     * Pulls low-overhead sync metrics.
     *
     * Parameters
     * ----------
     * gameId : String
     * Unique system target match identifier.
     *
     * Results
     * -------
     * GameUpdateResponse?
     * Slim telemetry packet snapshot reflecting live moves, or `null` if missing.
     */
    suspend fun getGameByIdServerResponse(gameId: String): GameUpdateResponse? {
        return gameRepo.getGameByIdServerResponse(gameId)
    }

    /**
     * Overwrites raw PGN notation move tracks for a designated match entry.
     *
     * Parameters
     * ----------
     * gameId : String
     * Unique operational row database reference key.
     * pgn : String
     * The sequential historical move text string replacing previous tracks.
     *
     * Results
     * -------
     * Boolean
     * `true` if transactional data alterations committed cleanly, `false` otherwise.
     */
    suspend fun updatePGN(gameId: String, pgn: String): Boolean {
        return gameRepo.updatePGNMoves(gameId, pgn)
    }

    /**
     * Modifies a move index location within an ongoing
     * game stream via an external engine connection proxy.
     *
     * Parameters
     * ----------
     * gameId : String
     * Target match key to edit.
     * moveIndex : Int
     * The concrete index location sequence number targeted for alteration.
     * moveSan : String
     * The replacement move text mapped using Standard Algebraic Notation (SAN) rules.
     *
     * Results
     * -------
     * Boolean
     * `true` if the remote gRPC engine validates and incorporates the correction, `false` otherwise.
     */
    suspend fun editMove(gameId: String, moveIndex: Int, moveSan: String):Boolean {
        return grpcClient.editMove(gameId, moveIndex, moveSan)
    }

    /**
     * Appends a new move to the move queue of an ongoing live match stream
     * using remote gRPC channel transport lines.
     *
     * Parameters
     * ----------
     * gameId : String
     * Target match key receiving the addition move.
     * moveSan : String
     * Standard Algebraic Notation (SAN) move token (e.g., "Nf3").
     *
     * Results
     * -------
     * Boolean
     * `true` if the real-time gameplay stream processes the move cleanly, `false` otherwise.
     */
    suspend fun addMove(gameId: String, moveSan: String): Boolean {
        return grpcClient.addMove(gameId, moveSan)
    }

    /**
     * Generates a completely new tournament match configuration record, configuring default
     * structural placeholders before actual live moves are detected.
     *
     * Parameters
     * ----------
     * roundId : String
     * Reference link string to the overarching round container record.
     * request : GameRequest
     * Inbound data transport blueprint establishing pairing rosters and board indexing parameters.
     * pgnHeader : String
     * Formatted metadata text tracking event descriptions, pairings, and round sequences.
     *
     * Results
     * -------
     * GameResponse
     * The generated and committed operational match status DTO.
     */
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

    /**
     * Gathers single user casual games independent of structured tournaments.
     *
     * Parameters
     * ----------
     * userId : Int
     * Numeric primary key identifying the target platform user account profile.
     *
     * Results
     * -------
     * List<IndividualGame>
     * Array gathering matched standalone match records where the user was logged.
     */
    suspend fun getGamesByUserId(userId: Int): List<IndividualGame> {
        return gameRepo.getIndividualGamesByPlayer(userId)
    }

    /**
     * Instantiates an isolated personal match entry.
     *
     * Parameters
     * ----------
     * individualGame : IndividualGameRequest
     * Structural data tracking configurations.
     *
     * Results
     * -------
     * IndividualGame
     * The fully assigned casual domain entity created in storage.
     */
    suspend fun createIndividualGame(individualGame: IndividualGameRequest): IndividualGame {
        val id = UUID.randomUUID().toString()
        return gameRepo.createIndividualGame(id, individualGame)
    }

    /**
     * Commits the finalized result to a match entry.
     *
     * Parameters
     * ----------
     * gameId : String
     * Unique system identification key for the target match.
     * result : String
     * Text token string representing outcomes (e.g. "1-0", "0-1", "1/2-1/2").
     *
     * Results
     * -------
     * Boolean
     * `true` if the state alteration saved cleanly to storage, `false` otherwise.
     */
    suspend fun updateResult(gameId: String, result: String): Boolean{
        return gameRepo.updateResult(gameId, result)
    }
}