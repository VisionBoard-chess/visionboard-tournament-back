package com.example


import com.example.auth.FirebaseAdmin
import com.example.auth.configureFirebaseAuth
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    FirebaseAdmin.initialize()
    val secret = environment.config.property("secret.key").getString()
    install(Authentication) {
        configureFirebaseAuth()
        bearer("python-api-key"){
            authenticate { tokenCredential ->
                if(tokenCredential.token == secret){
                    UserIdPrincipal("python-server")
                } else{
                    null
                }
            }

        }
    }
    configureSerialization()
    configureDatabases()
    configureHTTP()
    configureRouting()
}
