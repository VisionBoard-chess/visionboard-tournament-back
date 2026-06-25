package com.example.services

import com.example.repositories.RoundRepository
import com.example.models.*
import com.example.repositories.TournamentRepository
import java.time.LocalDateTime
import java.util.UUID

/**
 * RoundService
 * * Domain service component handling the lifecycle, business rules, and
 * multi-platform coordination for tournament rounds.
 *
 * This layer encapsulates transaction orchestrations for local data mapping,
 * PGN specification processing, and automated push synchronization to the
 * external Lichess broadcast API endpoints.
 *
 * Parameters
 * ----------
 * roundRepo : RoundRepository
 * Data gateway managing direct CRUD transactions on round-related records.
 * tournamentRepo : TournamentRepository
 * Data gateway managing read/write constraints on parent tournament configurations.
 * gameService : GameService
 * Business core engine driving discrete chess match setups and sub-state queries.
 * lichessService : LichessService
 * Network communication facade routing sync streams to the Lichess broadcast system.
 */
class RoundService(
    private val roundRepo: RoundRepository,
    private val tournamentRepo: TournamentRepository,
    private val gameService: GameService,
    private val lichessService: LichessService
) {
    /**
     * Provisions a brand new round record under an existing tournament structure,
     * initializing underlying games and mirroring telemetry to the Lichess network.
     *
     * Business Execution Chain:
     * 1. Rejects execution if the current sequence identifier is occupied.
     * 2. Extracts Lichess remote links and registers an isolated remote round asset.
     * 3. Commits an immutable `Round` infrastructure baseline locally.
     * 4. Synthesizes standardized PGN header payloads across child match vectors.
     * 5. Joins separate game records into a single multi-game PGN string block.
     * 6. Writes the finished token payload into local storage and transmits updates downstream.
     *
     * Parameters
     * ----------
     * tournamentId : String
     * Database structural UUID referencing the root tournament instance.
     * request : RoundRequest
     * Inbound data transport structure outlining naming, sequencing, and matching matrices.
     *
     * Results
     * -------
     * RoundResponse
     * Formatted structural DTO exposing consolidated round states and child components.
     *
     * Throws
     * ------
     * IllegalArgumentException
     * Raised if round order numbers collide or if the designated tournament parent
     * is missing.
     */
    suspend fun createRound(tournamentId: String, request: RoundRequest): RoundResponse {
        if (roundRepo.existsByTournamentAndNumber(tournamentId, request.roundNumber)) {
            throw IllegalArgumentException("Round number ${request.roundNumber} already exists for tournament $tournamentId")
        }
        val lichessBroadcastId = tournamentRepo.findById(tournamentId)?.lichessBroadcastId
            ?: throw IllegalArgumentException("Tournament not found")
        val lichessRoundId = lichessService.createRound(lichessBroadcastId, request.name)
        val tournamentName = tournamentRepo.findById(tournamentId)?.name
        val round = Round(
            roundId = UUID.randomUUID().toString(),
            tournamentId = tournamentId,
            name = request.name,
            roundNumber = request.roundNumber,
            pgn = "", // Placeholder, se actualizará después de crear el round en Lichess
            lichessRoundId = lichessRoundId,
            status = Status.NOT_STARTED.name,
            startDate = request.startDate?.let { LocalDateTime.parse(it) }
        )
        roundRepo.createRound(round)

        val games = request.games.map {
            val header = "[Event \"${tournamentName}\"]\n" +
                    "[Round \"${request.name + " " + request.roundNumber}\"]\n" +
                    "[White \"${it.white}\"]\n" +
                    "[Black \"${it.black}\"]"
            gameService.createGame(round.roundId, it, header)
        }
        val pgn = games.joinToString("\n\n"){it.pgn}
        roundRepo.updatePGN(round.roundId, pgn)
        lichessService.pushRoundPgn(lichessRoundId,pgn)
        return toResponse(round, games)
    }

    /**
     * Resolves an ordered list of rounds attached to a common tournament context,
     * filling in active relational game records dynamically.
     *
     * Parameters
     * ----------
     * tournamentId : String
     * Root identifier string of the parent tournament.
     *
     * Results
     * -------
     * List<RoundResponse>
     * A structured array collecting the mapped details of matched round records.
     */
    suspend fun getRoundsByTournamentId(tournamentId: String): List<RoundResponse> {
        return roundRepo.findByTournamentId(tournamentId).map {round ->
            val games = gameService.getGamesByRoundId(round.roundId)
            toResponse(round, games)
        }
    }

    /**
     * Pulls details for an individual round record verified by its system key.
     *
     * Parameters
     * ----------
     * roundId : String
     * Unique system identification key for the target round.
     *
     * Results
     * -------
     * RoundResponse?
     * The compiled operational round response DTO if found, otherwise returns `null`.
     */
    suspend fun getRoundById(roundId: String): RoundResponse? {
        val round = roundRepo.findById(roundId) ?: return null
        val games = gameService.getGamesByRoundId(roundId)
        return toResponse(round, games)

    }

    /**
     * Gathers current separate child PGN textual data nodes and overwrites
     * the centralized structural text sheet stream representation.
     *
     * Parameters
     * ----------
     * roundId : String
     * Structural key referencing the host round.
     *
     * Results
     * -------
     * Boolean?
     * Verification success status flag from persistent write layer,
     * or `null` if reference parameters map to a ghost record.
     */
    suspend fun syncRoundPgnFromGames(roundId: String): Boolean? {
        val games = gameService.getGamesByRoundId(roundId)
        val pgn = games.joinToString("\n\n") {it.pgn}
        return updatePGN(roundId, pgn)
    }

    /**
     * Alters local round PGN record layers and triggers push transmissions
     * across registered remote ecosystem brokers.
     *
     * Parameters
     * ----------
     * roundId : String
     * Unique key referencing the structural target database row.
     * pgn : String
     * Aggregated text string formatting standard chess data tracks.
     *
     * Results
     * -------
     * Boolean?
     * Operations completion flag, returning `null` if structural record target updates
     * mismatch.
     */
    suspend fun updatePGN(roundId: String, pgn: String): Boolean? {
        val round = roundRepo.findById(roundId) ?: return null
        val lichessRoundId = round.lichessRoundId
        val success = roundRepo.updatePGN(roundId, pgn)
        if (success) {
            lichessService.pushRoundPgn(lichessRoundId, pgn)
        }
        return success
    }

    private fun toResponse(round: Round, games: List<GameResponse> = emptyList()): RoundResponse = RoundResponse(
        roundId = round.roundId,
        tournamentId = round.tournamentId,
        name = round.name,
        roundNumber = round.roundNumber,
        status = round.status,
        startDate = round.startDate?.toString(),
        games = games
    )

    /**
     * Evaluation proxy identifying if concurrent sister round configurations
     * register active games.
     *
     * Parameters
     * ----------
     * tournamentId : String
     * Root identifier checking adjacent bounds.
     * roundId : String
     * Focus round identifier excluded from conflict queries.
     *
     * Results
     * -------
     * Boolean
     * Returns `true` if active target entries exist, otherwise returns `false`.
     */
    suspend fun hasAnotherActiveRound(tournamentId: String, roundId: String): Boolean {
        return roundRepo.hasAnotherActiveRound(tournamentId, roundId)
    }

    /**
     * Transitions operational state identifiers for an explicit round record and
     * recalculates tournament progression parameters retroactively.
     *
     * Parameters
     * ----------
     * roundId : String
     * Unique target round system identifier key.
     * tournamentId : String
     * Root parent tournament container link reference.
     * status : String
     * Target string name mapping to structural step designations.
     *
     * Results
     * -------
     * Boolean
     * Returns `true` if update tasks hit destination metrics cleanly, otherwise `false`.
     */
    suspend fun updateStatus(roundId: String, tournamentId: String, status: String): Boolean {
        val statusEnum = Status.valueOf(status)
        val updated = roundRepo.updateStatus(roundId, statusEnum)
        if(updated) {
            val rounds = roundRepo.findByTournamentId(tournamentId)
            val newTournamentStatus = computeTournamentStatus(rounds.map { it.status })
            tournamentRepo.updateTournamentStatus(tournamentId, newTournamentStatus)
        }
        return updated
    }

    /**
     * Evaluates state maps across internal rounds to determine a collective tournament status.
     *
     * State Resolution Criteria:
     * - Returns `NOT_STARTED` if the round array tracks empty loops.
     * - Returns `FINISHED` only when every element reads complete.
     * - Fallback state maps evaluate to `ACTIVE` under active elements or partial progression.
     *
     * Parameters
     * ----------
     * statuses : List<String>
     * String status collections gathered from child round records.
     *
     * Results
     * -------
     * Status
     * Resolved tournament execution lifecycle enum milestone.
     */
    private fun computeTournamentStatus(statuses: List<String>): Status {
        if (statuses.isEmpty()) return Status.NOT_STARTED
        if (statuses.all { it == "FINISHED" }) return Status.FINISHED
        return Status.ACTIVE
    }
}