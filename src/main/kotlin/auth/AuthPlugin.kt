package com.example.auth

import io.ktor.server.auth.*

const val FirebaseAuthKey = "firebase-jwt"

class FirebaseAuthPrincipal(firebaseUid: String)

fun AuthenticationConfig.configureFirebaseAuth(){
    bearer(FirebaseAuthKey){
        authenticate { tokenCredential ->
            val token = tokenCredential.token
            val firebaseUid = FirebaseAdmin.verifyToken(token)
            if (firebaseUid != null) {
                FirebaseAuthPrincipal(firebaseUid)
            } else {
                null
            }
        }
    }
}