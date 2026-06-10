package com.example.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.io.FileInputStream

object FirebaseAdmin {

    fun initialize(){
        if(FirebaseApp.getApps().isEmpty()){
            val serviceAccount = FileInputStream("serviceAccountKey.json")

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            FirebaseApp.initializeApp(options)
            println("FirebaseApp initialized.")
        }
    }

    fun verifyToken(token: String): String?{
        return try{
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)

            val firebaseInfo = decodedToken.claims["firebase"] as? Map<*, *>
            val signInProvider = firebaseInfo?.get("sign_in_provider")?.toString()

            val isAnnonymous = signInProvider == "anonymous"

            if (!isAnnonymous && !decodedToken.isEmailVerified){
                println(">>> Bloqueado: ${decodedToken} no tiene email verificado.")
                return null
            }

            decodedToken.uid
        }
        catch (e: Exception){
            println(">>> Error al verificar token: ${e.message}")
            null
        }
    }
}