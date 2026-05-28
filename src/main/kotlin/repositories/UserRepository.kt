package com.example.repositories

import com.example.models.UserRequest
import com.example.models.UserRequestUpdate
import com.example.models.UserResponse
import com.example.tables.UserTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UserRepository {
    suspend fun createUser(user: UserRequest): UserResponse = dbQuery { //revisar la creacion de usuarios
        UserTable.insert {
            it[firebaseUid] = user.firebaseUid
            it[nickname] = user.nickname
        }
        UserTable.selectAll()
            .where { UserTable.firebaseUid eq user.firebaseUid }
            .map { rowToUser(it) }
            .single()
    }
    suspend fun getByFirebaseUid(firebaseUid: String):UserResponse = dbQuery {
        UserTable.selectAll()
            .where (UserTable.firebaseUid eq firebaseUid)
            .map { rowToUser(it) }
            .single()
    }

    suspend fun getUserById(id: Int): UserResponse = dbQuery {
        UserTable.selectAll()
            .where (UserTable.id eq id)
            .map { rowToUser(it) }
            .single()
    }

    suspend fun getUserByNickname(nickname: String): UserResponse = dbQuery {
        UserTable.selectAll()
            .where (UserTable.nickname eq nickname)
            .map { rowToUser(it) }
            .single()
    }

    suspend fun deleteByFirebaseUid(firebaseUid: String) { //revisar esto
        dbQuery {
            UserTable.deleteWhere { UserTable.firebaseUid eq firebaseUid }
        }
    }

    suspend fun updateById(request: UserRequestUpdate): UserResponse = dbQuery {
        UserTable.update({ UserTable.id eq request.id }) {
            it[nickname] = request.nickname
        }
        UserTable.selectAll()
            .where { UserTable.id eq request.id }
            .map { rowToUser(it) }
            .single()
    }

    suspend fun getAllUsers(): List<UserResponse> = dbQuery {
        UserTable.selectAll()
            .map { rowToUser(it) }
    }

    suspend fun nicknameExists(nickname: String): Boolean = dbQuery {
        UserTable.selectAll().where { UserTable.nickname eq nickname }.count() > 0
    }


    private fun rowToUser(it: ResultRow): UserResponse {
        return UserResponse(
            nickname = it[UserTable.nickname],
            id = it[UserTable.id]
        )
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }



}