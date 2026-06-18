package com.example.services

import com.example.repositories.RoundRepository
import com.example.models.*
import com.example.repositories.TournamentRepository
import java.time.LocalDateTime
import java.util.UUID

class RoundService(
    private val roundRepo: RoundRepository,
    private val tournamentRepo: TournamentRepository,
    private val gameService: GameService,
    private val lichessService: LichessService
) {
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

    suspend fun getRoundsByTournamentId(tournamentId: String): List<RoundResponse> {
        return roundRepo.findByTournamentId(tournamentId).map {round ->
            val games = gameService.getGamesByRoundId(round.roundId)
            toResponse(round, games)
        }
    }

    suspend fun getRoundById(roundId: String): RoundResponse? {
        val round = roundRepo.findById(roundId) ?: return null
        val games = gameService.getGamesByRoundId(roundId)
        return toResponse(round, games)

    }

    suspend fun syncRoundPgnFromGames(roundId: String): Boolean? {
        val games = gameService.getGamesByRoundId(roundId)
        val pgn = games.joinToString("\n\n") {it.pgn}
        return updatePGN(roundId, pgn)
    }

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

    suspend fun hasAnotherActiveRound(tournamentId: String, roundId: String): Boolean {
        return roundRepo.hasAnotherActiveRound(tournamentId, roundId)
    }

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

    private fun computeTournamentStatus(statuses: List<String>): Status {
        if (statuses.isEmpty()) return Status.NOT_STARTED // inicio not_started
        if (statuses.all { it == "FINISHED" }) return Status.FINISHED // cuando todas las rondas creadas se han finalizado
        return Status.ACTIVE // si hay al menos una ronda activa o no todas las rondas están finalizadas
    }
}