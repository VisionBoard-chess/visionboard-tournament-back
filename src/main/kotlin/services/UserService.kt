package com.example.services

import com.example.models.UserRequest
import com.example.models.UserRequestUpdate
import com.example.models.UserResponse
import com.example.repositories.UserRepository

class UserService(private val repo: UserRepository) {
    suspend fun create(user: UserRequest): UserResponse{
        return repo.createUser(user)
    }

    suspend fun getByFirebaseUid(firebaseUid: String): UserResponse {
        return repo.getByFirebaseUid(firebaseUid)
    }

    suspend fun deleteByFirebaseUid(firebaseUid: String) {
        return repo.deleteByFirebaseUid(firebaseUid)
    }

    suspend fun updateUser(request: UserRequestUpdate):UserResponse {
        return repo.updateById(request)
    }

    suspend fun getAllUsers(): List<UserResponse> {
        return repo.getAllUsers()
    }

    suspend fun nicknameExists(nickname: String):Boolean {
        return repo.nicknameExists(nickname)
    }
}