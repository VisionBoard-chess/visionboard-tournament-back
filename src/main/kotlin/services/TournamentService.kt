package com.example.services

import com.example.models.Tournament
import com.example.models.TournamentPublicResponse
import com.example.models.TournamentRequest
import com.example.models.TournamentResponse
import com.example.repositories.TournamentRepository
import java.time.LocalDateTime
import java.util.UUID

class TournamentService(private val repo: TournamentRepository, private val lichessService: LichessService) {

    private fun generateAccessCode(): String {
        val characters = ('A'..'Z') + ('0'..'9')
        return (1..6).map { characters.random() }.joinToString("")
    }



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

    suspend fun getById(tournamentId: String): TournamentPublicResponse? {
        val tournament = repo.findById(tournamentId) ?: return null
        return toPublicResponse(tournament)
    }

    suspend fun getAll(): List<TournamentPublicResponse> {
        return repo.getAll().map { toPublicResponse(it) }
    }

    suspend fun getByCreatorId(creatorId: String): List<TournamentResponse> {
        return repo.findByCreatorId(creatorId).map { toResponse(it) }
    }

    suspend fun delete(tournamentId: String): Boolean {
        return repo.deleteTournament(tournamentId)
    }
}