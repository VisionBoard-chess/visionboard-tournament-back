package com.example.routes

import com.example.models.UserRequest
import com.example.models.UserRequestUpdate
import com.example.services.UserService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {
    route("/user") {
        get("/check-nickname") {
            val nickname = call.request.queryParameters["nickname"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing nickname")
            val exists = userService.nicknameExists(nickname)
            call.respond(if (exists) HttpStatusCode.Conflict else HttpStatusCode.OK)
        }
        get {
            println(">>> GET /user")
            val users = userService.getAllUsers()
            call.respond(HttpStatusCode.OK, users)
        }
        post("/register") {
            val request = call.receive<UserRequest>()
            println(">>> POST /user/register")
            val user = userService.create(request)
            println(">>> Usuario registrado: $user")
            call.respond(HttpStatusCode.Created, user)
        }
        get("/{firebaseUid}"){
            val firebaseUid = call.parameters["firebaseUid"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            println(">>> GET /user/$firebaseUid")
            val user = userService.getByFirebaseUid(firebaseUid)
            call.respond(HttpStatusCode.OK, user)
        }
        delete("/{firebaseUid}"){
            val firebaseUid = call.parameters["firebaseUid"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            userService.deleteByFirebaseUid(firebaseUid)
            call.respond(HttpStatusCode.OK)
        }
        put("/update/{id}"){
            val request = call.receive<UserRequestUpdate>()
            val user = userService.updateUser(request)
        }
    }
}