package com.example.services

import com.example.models.UserRequest
import com.example.models.UserRequestUpdate
import com.example.models.UserResponse
import com.example.repositories.UserRepository

/**
 * UserService
 * * Domain service component orchestrating user identity lifecycles, profile telemetry updates,
 * data clearing operations, and registration constraints.
 *
 * This layer abstracts data mapping and validations between presentation requests
 * and storage systems.
 *
 * Parameters
 * ----------
 * repo : UserRepository
 * Data gateway orchestrating direct persistence transactions on user records.
 */
class UserService(private val repo: UserRepository) {

    /**
     * Registers and commits a completely new user identity record to storage.
     *
     * Parameters
     * ----------
     * user : UserRequest
     * Inbound data transport blueprint containing configuration defaults, emails,
     * and chosen profile attributes.
     *
     * Results
     * -------
     * UserResponse
     * Formatted structural DTO exposing the newly persisted user profile snapshot.
     */
    suspend fun create(user: UserRequest): UserResponse{
        return repo.createUser(user)
    }

    /**
     * Resolves an individual user identity context verified by FirebaseUid.
     *
     * Parameters
     * ----------
     * firebaseUid : String
     * Unique client identity string assigned by the external Firebase Auth subsystem.
     *
     * Results
     * -------
     * UserResponse
     * The verified matching user profile details record.
     */
    suspend fun getByFirebaseUid(firebaseUid: String): UserResponse {
        return repo.getByFirebaseUid(firebaseUid)
    }

    /**
     * Removes an entire user profile directory branch from persistent database tables
     * using their authentication token link.
     *
     * Parameters
     * ----------
     * firebaseUid : String
     * Unique client identity string targeted for structural record destruction.
     *
     * Results
     * -------
     * Unit
     * This function executes database mutation operations as side effects and
     * does not return an explicit value.
     */
    suspend fun deleteByFirebaseUid(firebaseUid: String) {
        return repo.deleteByFirebaseUid(firebaseUid)
    }

    /**
     * Modifies mutable user metadata properties, replacing stale attributes with
     * updated payload blocks.
     *
     * Parameters
     * ----------
     * request : UserRequestUpdate
     * Inbound data envelope containing target identifiers along with altered property definitions.
     *
     * Results
     * -------
     * UserResponse
     * The updated user state representation model returned upon successful transaction commit.
     */
    suspend fun updateUser(request: UserRequestUpdate):UserResponse {
        return repo.updateById(request)
    }

    /**
     * Pulls a comprehensive listing of all registered application users.
     *
     * Results
     * -------
     * List<UserResponse>
     * A collection array accumulating presentation DTO snapshots for all system accounts.
     */
    suspend fun getAllUsers(): List<UserResponse> {
        return repo.getAllUsers()
    }

    /**
     * Validation check identifying if an alphanumeric screen name or handle is occupied
     * within the system.
     *
     * Used to enforce unique naming conventions during onboarding or profile editing workflows.
     *
     * Parameters
     * ----------
     * nickname : String
     * The candidate text string under verification.
     *
     * Results
     * -------
     * Boolean
     * `true` if the handle already exists in data records, `false` if it is free to use.
     */
    suspend fun nicknameExists(nickname: String):Boolean {
        return repo.nicknameExists(nickname)
    }
}