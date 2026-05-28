package com.example.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object TournamentTable : Table("tournaments"){
    val id = varchar("tournament_id", 36)
    val name = varchar("name", 255)
    val description = text("description").default("")
    val typeOf = varchar("type_of", 50).default("OPEN")
    val startDate = datetime("start_date").default(LocalDateTime.now())
    val creatorId = integer("creator_id").references(UserTable.id)
    val accessCode = varchar("access_code", 10)
    val lichessBroadcastId = varchar("lichess_broadcast_id", 255)
    val status = varchar("status", 36).default("NOT_STARTED")

    override val primaryKey = PrimaryKey(id)
}