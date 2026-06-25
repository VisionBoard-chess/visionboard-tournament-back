package com.example.services

import com.example.models.Tournament
import com.example.models.TournamentPublicResponse
import com.example.models.TournamentRequest
import com.example.models.TournamentResponse
import com.example.repositories.TournamentRepository
import java.time.LocalDateTime
import java.util.UUID

/**
 * TournamentService
 * * Domain service component coordinating tournament configurations, administrative views,
 * data protection filters, and external broadcast configurations.
 *
 * This layer is responsible for creating unique tournament tokens, managing data mapper
 * boundaries between public and private user scopes, and initializing real-time event
 * pipelines within the Lichess infrastructure.
 *
 * Parameters
 * ----------
 * repo : TournamentRepository
 * Data gateway orchestrating direct persistence transactions on tournament records.
 * lichessService : LichessService
 * Network facade routing configurations to the Lichess broadcast system API.
 */
class TournamentService(private val repo: TournamentRepository, private val lichessService: LichessService) {

    /**
     * Generates a random alphanumeric authentication token used as a security gateway.
     *
     * Combines capital letters [A-Z] and single digits [0-9] into a 6-character
     * string value used to capture images of a game via app.
     *
     * Results
     * -------
     * String
     * A 6-character alphanumeric sequence string (e.g., "A8X29B").
     */
    private fun generateAccessCode(): String {
        val characters = ('A'..'Z') + ('0'..'9')
        return (1..6).map { characters.random() }.joinToString("")
    }

    /**
     * Requests the provisioning of an external streaming broadcast link on Lichess.
     *
     * Parameters
     * ----------
     * name : String
     * The title designated for the live broadcast event stream.
     *
     * Results
     * -------
     * String
     * The unique remote broadcast identifier returned by the Lichess network configuration.
     */
    private suspend fun createLichessBroadcast(name: String): String {
        return lichessService.createBroadcast(name)
    }

    private fun toResponse(tournament: Tournament) = TournamentResponse (
        tournamentId = tournament.tournamentId,
        name = tournament.name,
        description = tournament.description,
        typeOf = tournament.typeOf,
        startDate = tournament.startDate.toString(),
        creatorId = tournament.creatorId,
        accessCode = tournament.accessCode,
        lichessBroadcastId = tournament.lichessBroadcastId,
        roundIds = tournament.roundIds,
        status = tournament.status
    )

    private fun toPublicResponse(tournament: Tournament) = TournamentPublicResponse (
        tournamentId = tournament.tournamentId,
        name = tournament.name,
        description = tournament.description,
        typeOf = tournament.typeOf,
        startDate = tournament.startDate.toString(),
        roundIds = tournament.roundIds,
        status = tournament.status
    )

    /**
     * Initializes and registers a completely new tournament system configuration,
     * establishing data links to both local storage and external broadcast networks.
     *
     * Parameters
     * ----------
     * request : TournamentRequest
     * Inbound data transport structure carrying event labels, scheduling timelines,
     * and creator profile keys.
     *
     * Results
     * -------
     * TournamentResponse
     * The completed administrative tournament profile DTO containing the newly generated
     * authorization configurations.
     */
    suspend fun create(request: TournamentRequest): TournamentResponse {
        val broadcastId = createLichessBroadcast(request.name)
        val tournament = Tournament(
            tournamentId = UUID.randomUUID().toString(),
            name = request.name,
            description = request.description,
            typeOf = request.typeOf,
            startDate = LocalDateTime.parse(request.startDate),
            creatorId = request.creatorId,
            accessCode = generateAccessCode(),
            lichessBroadcastId = broadcastId, // Placeholder, añadir conexion con Lichess API para obtener el ID real del broadcast
            status = "NOT_STARTED"
        )
        repo.createTournament(tournament)
        return toResponse(tournament)
    }

    /**
     * Resolves a tournament context filtered under public visibility restrictions.
     *
     * Parameters
     * ----------
     * tournamentId : String
     * Unique operational row database reference key.
     *
     * Results
     * -------
     * TournamentPublicResponse?
     * The filtered public tournament details packet, or `null` if no records are matched.
     */
    suspend fun getById(tournamentId: String): TournamentPublicResponse? {
        val tournament = repo.findById(tournamentId) ?: return null
        return toPublicResponse(tournament)
    }

    /**
     * Retrieves a listing of all active and historical tournaments scrubbed for
     * public view distribution.
     *
     * Results
     * -------
     * List<TournamentPublicResponse>
     * A collection array collecting all public tournament overview silhouettes.
     */
    suspend fun getAll(): List<TournamentPublicResponse> {
        return repo.getAll().map { toPublicResponse(it) }
    }

    /**
     * Resolves an administrative-clearance tournament collection array created by
     * an authorized system account.
     *
     * Parameters
     * ----------
     * creatorId : Int
     * Numerical system primary key tracking the owning user profile account.
     *
     * Results
     * -------
     * List<TournamentResponse>
     * An collection array of full administrative payloads matching the target owner key.
     */
    suspend fun getByCreatorId(creatorId: Int): List<TournamentResponse> {
        return repo.findByCreatorId(creatorId).map { toResponse(it) }
    }

    /**
     * Removes an entire tournament directory branch from persistent database tables.
     *
     * Parameters
     * ----------
     * tournamentId : String
     * Unique identification tracking key targeted for deletion.
     *
     * Results
     * -------
     * Boolean
     * `true` if transactional row destruction commands finished cleanly, `false` otherwise.
     */
    suspend fun delete(tournamentId: String): Boolean {
        return repo.deleteTournament(tournamentId)
    }
}