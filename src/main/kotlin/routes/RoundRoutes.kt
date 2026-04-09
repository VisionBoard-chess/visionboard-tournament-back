package com.example.routes

import com.example.models.RoundRequest
import com.example.services.TournamentService
import com.example.services.RoundService

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configures the endpoints related to the management of rounds within a tournament.
 *
 * Will expose these endpoints under the base path `/tournaments/{tournamentId}/rounds`:
 * - `POST /` : Creates and adds a new round (along with its games) to the specified tournament.
 * - `GET /` : Retrieves all rounds associated with a specific tournament.
 * - `GET /next-round-number` : Calculates and returns the next available round number for creation.
 * - `GET /{roundId}` : Gets the details of a specific round.
 *
 * @param roundService Instance of [RoundService] in charge of business logic and persistence for rounds.
 * @param tournamentService Instance of [TournamentService] used to validate the existence of the associated tournament.
 */
fun Route.roundRoutes(roundService: RoundService, tournamentService: TournamentService) {

    route("/tournaments/{tournamentId}/rounds") {
        post {
            val tournamentId = call.parameters["tournamentId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                "Missing tournamentId"
            )
            val tournament = tournamentService.getById(tournamentId) ?: return@post call.respond(
                HttpStatusCode.NotFound,
                "Tournament not found"
            )
            val request = call.receive<RoundRequest>()
            println(">>> POST /tournaments/$tournamentId/rounds - Request recibido: $request")
            try {
                val round = roundService.createRound(tournamentId, request)
                println(">>> Ronda creada: $round")
                call.respond(HttpStatusCode.Created, round)
            } catch (e: IllegalArgumentException) {
                println(">>> Error al crear ronda: ${e.message}")
                call.respond(HttpStatusCode.Conflict, e.message ?: "Conflict")
            }
        }
        get {
            val tournamentId = call.parameters["tournamentId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing tournamentId")
            println(">>> GET /tournaments/$tournamentId/rounds")
            val rounds = roundService.getRoundsByTournamentId(tournamentId)
            println(">>> Rondas encontradas para torneo $tournamentId: ${rounds.size}")
            call.respond(HttpStatusCode.OK, rounds)
        }
        get("/next-round-number"){
            val tournamentId = call.parameters["tournamentId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing tournamentId")
            println(">>> GET /tournaments/$tournamentId/next-round-number")
            val rounds = roundService.getRoundsByTournamentId(tournamentId)
            val usedNumbers = rounds.map { it.roundNumber }.toSortedSet()
            var nextNumber = 1
            while(usedNumbers.contains(nextNumber)) nextNumber++
            call.respond(HttpStatusCode.OK, mapOf("nextRoundNumber" to nextNumber))
        }
        get("/{roundId}") {
            val roundId = call.parameters["roundId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing roundId")
            println(">>> GET /tournaments/{tournamentId}/rounds/$roundId")
            val round = roundService.getRoundById(roundId) ?: return@get call.respond(HttpStatusCode.NotFound, "Round not found")
            println(">>> Ronda encontrada: $round")
            call.respond(HttpStatusCode.OK, round)
        }
        

        //put para actualizar ronda (status o pgn)
        //tal vez otro get para diferenciar quien puede ver las rondas y tal (solo por creador?)
    }
}