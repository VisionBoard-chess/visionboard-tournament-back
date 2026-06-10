package com.example

import com.example.client.GrpcGameClient
import com.example.database.DatabaseFactory
import com.example.repositories.GameRepository
import com.example.repositories.RoundRepository
import com.example.repositories.TournamentRepository
import com.example.repositories.UserRepository
import com.example.routes.protectedUserRoutes
import com.example.routes.publicUserRoutes
import com.example.routes.roundRoutes
import com.example.services.TournamentService
import com.example.routes.tournamentRoutes
import com.example.services.GameService
import com.example.services.LichessService
import com.example.services.RoundService
import com.example.services.UserService
import io.ktor.server.application.*
import io.ktor.server.routing.*


fun Application.configureDatabases() {
    val lichessToken = environment.config.property("lichess.token").getString()

    val dbUrl = environment.config.property("postgres.url").getString()
    val dbUser = environment.config.property("postgres.user").getString()
    val dbPassword = environment.config.property("postgres.password").getString()

    DatabaseFactory.init(dbUrl, dbUser, dbPassword)

    val lichessService = LichessService(lichessToken)
    val tournamentService = TournamentService(TournamentRepository(), lichessService)
    val gameService = GameService(GameRepository(), GrpcGameClient())
    val roundService = RoundService(RoundRepository(), TournamentRepository(),  gameService, lichessService)
    val userService = UserService(UserRepository())

    routing {
        publicUserRoutes(userService)
        publicGameRoutes(gameService)
        authenticate(FirebaseAuthKey, "python-api-key") {
            tournamentRoutes(tournamentService)
            roundRoutes(roundService, tournamentService)
            privateGameRoutes(gameService, roundService)
            protectedUserRoutes(userService)
        }
    }
}