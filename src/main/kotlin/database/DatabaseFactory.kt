package com.example.database

import com.example.tables.GameTable
import com.example.tables.RoundTable
import com.example.tables.TournamentTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(dbUrl: String, dbUser: String, dbPassword: String): Database {
        val database = Database.connect(
            dbUrl,
            driver = "org.postgresql.Driver",
            user = dbUser,
            password = dbPassword
        )
        transaction(database) {
            SchemaUtils.create(TournamentTable)
            SchemaUtils.create(RoundTable)
            SchemaUtils.create(GameTable)
        }
        return database
    }

}