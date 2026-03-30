package com.example.tables

import org.jetbrains.exposed.sql.Table

object RoundTable: Table("rounds") {
    val roundId = varchar("round_id", 36)
    val tournamentId = varchar("tournament_id", 36).references(TournamentTable.id)
    val name = varchar("name", 255)
    val roundNumber = integer("round_number")
    val pgn = text("pgn").default("")
    val lichessRoundId = varchar("lichess_round_id", 255).default("")
    val status = varchar("status", 36).default("NOT_STARTED")
    val startDate = varchar("start_date",50).nullable()

    override val primaryKey = PrimaryKey(roundId)
}