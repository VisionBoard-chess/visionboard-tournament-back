package com.example.models

import kotlinx.serialization.Serializable

data class User(
    val id: Int,
    val firebaseUid: String,
    val nickname: String
)

@Serializable
data class UserRequest(
    val firebaseUid: String,
    val nickname: String
)
@Serializable
data class UserRequestUpdate(
    val id: Int,
    val nickname: String
)
@Serializable
data class UserResponse(
    val id: Int,
    val nickname: String
)