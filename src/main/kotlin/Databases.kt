package com.example

import com.example.client.GrpcGameClient
import com.example.database.DatabaseFactory
import com.example.repositories.GameRepository
import com.example.repositories.RoundRepository
import com.example.repositories.TournamentRepository
import com.example.routes.gameRoutes
import com.example.routes.gameSubscribers
import com.example.routes.roundRoutes
import com.example.services.TournamentService
import com.example.routes.tournamentRoutes
import com.example.services.GameService
import com.example.services.LichessService
import com.example.services.RoundService
import io.ktor.server.application.*
import io.ktor.server.routing.*


fun Application.configureDatabases() {
    val lichessToken = environment.config.property("lichess.token").getString()
    DatabaseFactory.init()

    val lichessService = LichessService(lichessToken)
    val tournamentService = TournamentService(TournamentRepository(), lichessService)
    val gameService = GameService(GameRepository(), GrpcGameClient())
    val roundService = RoundService(RoundRepository(), TournamentRepository(),  gameService, lichessService)

    routing {
        tournamentRoutes(tournamentService)
        roundRoutes(roundService, tournamentService)
        gameRoutes(gameService, roundService)
    }
}
