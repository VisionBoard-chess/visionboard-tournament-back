package com.example.database

import com.example.tables.GameTable
import com.example.tables.RoundTable
import com.example.tables.TournamentTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(): Database {
        val database = Database.connect(
            "jdbc:postgresql://localhost:5432/postgres",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "7045"
        )
        transaction(database) {
            SchemaUtils.create(TournamentTable)
            SchemaUtils.create(RoundTable)
            SchemaUtils.create(GameTable)
        }
        return database
    }

}