package com.example.routes

import com.example.models.TournamentRequest
import com.example.services.TournamentService

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.collections.get
import kotlin.text.get

fun Route.tournamentRoutes(service: TournamentService) {
    route("/tournaments") {
        post {
            val request = call.receive<TournamentRequest>()
            println(">>> POST /tournaments - Request recibido: $request")
            val tournament = service.create(request)
            println(">>> Tournament creado: $tournament")
            call.respond(HttpStatusCode.Created, tournament)
        }
        get {
            println(">>> GET /tournaments")
            val tournaments = service.getAll()
            println(">>> Torneos encontrados: ${tournaments.size}")
            call.respond(HttpStatusCode.OK, tournaments)
        }
        get("/{tournamentId}") {
            val tournamentId = call.parameters["tournamentId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing tournamentId")
            println(">>> GET /tournaments/$tournamentId")
            val tournament = service.getById(tournamentId) ?: return@get call.respond(HttpStatusCode.NotFound, "Tournament not found")
            println(">>> Tournament encontrado: $tournament")
            call.respond(HttpStatusCode.OK, tournament)
        }
        delete("/{tournamentId}") {
            val tournamentId = call.parameters["tournamentId"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing tournamentId")
            println(">>> DELETE /tournaments/$tournamentId")
            val success = service.delete(tournamentId)
            if (success) {
                println(">>> Tournament $tournamentId eliminado")
                call.respond(HttpStatusCode.NoContent)
            } else {
                println(">>> Tournament $tournamentId NO encontrado")
                call.respond(HttpStatusCode.NotFound, "Tournament not found")
            }
        }
        get("/creator/{creatorId}") {
            val creatorId = call.parameters["creatorId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing creatorId")
            println(">>> GET /tournaments/creator/$creatorId")
            val tournaments = service.getByCreatorId(creatorId)
            println(">>> Torneos del creador $creatorId: ${tournaments.size}")
            call.respond(HttpStatusCode.OK, tournaments)
        }
    }
}
