package com.example.helpers

import com.example.routes.privateGameRoutes
import com.example.routes.protectedUserRoutes
import com.example.routes.publicGameRoutes
import com.example.routes.publicUserRoutes
import com.example.routes.roundRoutes
import com.example.routes.tournamentRoutes
import com.example.services.GameService
import com.example.services.RoundService
import com.example.services.TournamentService
import com.example.services.UserService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun Application.testModule(
    userService: UserService? = null,
    tournamentService: TournamentService? = null,
    roundService: RoundService? = null,
    gameService: GameService? = null,
    ) {
    install(ContentNegotiation) { json() }
    install(Authentication) {
        bearer("firebase") {
            authenticate { UserIdPrincipal("test-user") }
        }
        bearer("python-api-key") {
            authenticate { UserIdPrincipal("python-server") }
        }
    }
    routing {
        userService?.let {
            publicUserRoutes(it)
            authenticate("firebase") { protectedUserRoutes(it) }
        }
        tournamentService?.let {
            authenticate("firebase") { tournamentRoutes(it) }
        }
        if (roundService != null && tournamentService != null) {
            authenticate("firebase") { roundRoutes(roundService, tournamentService) }
        }
        gameService?.let { gs ->
            publicGameRoutes(gs)
            roundService?.let { rs ->
                authenticate("firebase") { privateGameRoutes(gs, rs) }
            }
        }
    }
}