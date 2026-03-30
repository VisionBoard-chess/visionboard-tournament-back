package com.example.routes

import com.example.models.Game
import com.example.models.GameRequest
import com.example.repositories.GameRepository
import com.example.services.GameService
import com.example.services.RoundService
import com.example.tables.GameTable.gameId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.sse
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.sse.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet


val gameSubscribers = ConcurrentHashMap<String, CopyOnWriteArraySet<kotlinx.coroutines.channels.Channel<String>>>()

fun Route.gameRoutes(gameService: GameService, roundService: RoundService) {
    route("/games") {
        get("/tournament/{accessCode}") { //este para la conexion con el telefono
            val accessCode = call.parameters["accessCode"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing access code")
            println(">>> GET /games/tournament/$accessCode")
            val games = gameService.getGamesByAccessCode(accessCode)
            println(">>> Juegos encontrados para torneo $accessCode: ${games.size}")
            call.respond(HttpStatusCode.OK, games)
        }

        get("/round/{roundId}"){ //esto creo que tampoco se usa
            val roundId = call.parameters["roundId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing round ID")
            println(">>> GET /games/round/$roundId")
            val games = gameService.getGamesByRoundId(roundId)
            println(">>> Juegos encontrados para ronda $roundId: ${games.size}")
            call.respond(HttpStatusCode.OK, games)
        }
        //este getter tal vez sobra, ya que con el SSE ya obtengo la info de la partida?
        get("/{gameId}") { //puede que esto tenga que ser el websocket? para que se vaya acctualizando el pgn de game cada vez que se hace un put desde el otro backend? no lo tengo claro la vd tengo que mirarlo bien. O tal vez se tiene que enviar rollo por tiempo o algo asi
            val gameId = call.parameters["gameId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing game ID")
            println(">>> GET /games/$gameId")
            val game = gameService.getGameById(gameId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Game not found")
            println(">>> Juego encontrado: $game")
            call.respond(HttpStatusCode.OK, game)
        }

        post("/round/{roundId}") { //esto no sirve (nadie va a llamar a este endpoint)
            val roundId = call.parameters["roundId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing round ID")
            val request = call.receive<GameRequest>()
            println(">>> POST /games/round/$roundId - Request recibido: $request")
            val today = LocalDateTime.now()
            val formatted = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val header = "[Event \"Casual Game\"]\n" +
                    "[Date \"${formatted}\"]\n" +
                    "[White \"${request.white}\"]\n" +
                    "[Black \"${request.black}\"]"
            val game = gameService.createGame(roundId, request, header)
            println(">>> Juego creado: $game")
            call.respond(HttpStatusCode.Created, game)
        }

        put("/{gameId}/pgn") {
            val gameId = call.parameters["gameId"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing game ID")
            val pgnUpdate = call.receive<Map<String, String>>()
            val newPgn =
                pgnUpdate["pgn"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing PGN in request body")
            println(">>> PUT /games/$gameId/pgn - New PGN: $newPgn")
            val success = gameService.updatePGN(gameId, newPgn)
            if (success) {
                val game = gameService.getGameByIdServerResponse(gameId)
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Game not found after PGN update")
                roundService.syncRoundPgnFromGames(game.roundId)
                println(">>> PGN actualizado para juego $gameId")
                gameSubscribers[gameId]?.forEach { channel ->
                    channel.trySend(Json.encodeToString(
                        mapOf("type" to "pgn_update", "newPgn" to game?.pgn)
                    ))
                }
                call.respond(HttpStatusCode.OK, "PGN updated successfully")
            } else {
                println(">>> Juego $gameId NO encontrado para actualizar PGN")
                call.respond(HttpStatusCode.NotFound, "Game not found")
            }
        }

        put("/{gameId}/move/edit") {
            println(">>> PUT /games/$gameId/move/edit")
            val gameId = call.parameters["gameId"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing game ID")
            val body = call.receive<Map<String, String>>()
            val moveIndex = body["moveIndex"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing or invalid moveIndex")
            val moveSan = body["moveSan"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing moveSan")
            println("moveIndex: $moveIndex and moveSan: $moveSan")
            val success = gameService.editMove(gameId, moveIndex, moveSan)
            println("success: $success")
            if (success) call.respond(HttpStatusCode.OK, mapOf("success" to "true"))
            else call.respond(HttpStatusCode.BadRequest, mapOf("success" to "false", "error" to "Move rejected"))
        }

        put("/{gameId}/move/add") {
            val gameId = call.parameters["gameId"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing game ID")
            val body = call.receive<Map<String, String>>()
            val moveSan = body["moveSan"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing moveSan")
            val success = gameService.addMove(gameId, moveSan)
            if (success) call.respond(HttpStatusCode.OK, mapOf("success" to "true"))
            else call.respond(HttpStatusCode.BadRequest, mapOf("success" to "false", "error" to "Move rejected"))
        }
        get("/{gameId}/sse"){
            val gameId = call.parameters["gameId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing game ID")
            val game = gameService.getGameById(gameId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Game not found")
            println(">>> GET /games/$gameId/sse")

            call.response.headers.append("Content-Type", "text/event-stream")
            call.response.headers.append("Cache-Control", "no-cache")
            call.response.headers.append("Connection", "keep-alive")

            val channel = kotlinx.coroutines.channels.Channel<String>()
            gameSubscribers.getOrPut(gameId) { CopyOnWriteArraySet() }.add(channel)
            println(">>> Cliente suscrito a SSE para juego $gameId")

            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                write("data: ${Json.encodeToString(mapOf("type" to "initial_pgn", "pgn" to game.pgn))}\n\n")
                flush()
                try {
                    for (update in channel) {
                        write("data: $update\n\n")
                        flush()
                    }
                } finally {
                    gameSubscribers[gameId]?.remove(channel)
                    channel.close()
                    println(">>> Cliente desconectado de SSE para juego $gameId")
                }
            }

        }
        /* Mirar el WebSocket para enviar los cambios recurrentes del pgn, ya sea por deteccion o por edición o addición
        webSocket("/{gameId}/ws"){
            val gameId = call.parameters["gameId"] ?: run {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing gameId"))
                return@webSocket
            }
            gameSubscribers.getOrPut(gameId) { CopyOnWriteArraySet() }.add(this)

            gameService.getGameById(gameId)?.let { game ->
                send(Frame.Text(Json.encodeToString(mapOf("pgn" to game.pgn))))
            }

            try {
              incoming.consumeEach {}
            } finally {
                gameSubscribers[gameId]?.remove(this)
            }
        }*/
    }
}