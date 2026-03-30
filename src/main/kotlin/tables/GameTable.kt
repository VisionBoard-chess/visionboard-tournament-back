package com.example.tables

import org.jetbrains.exposed.sql.Table
object GameTable : Table("games") {
    val gameId = varchar("game_id", 36)
    val roundId = varchar("round_id", 36).references(RoundTable.roundId)
    val tableNumber = integer("table_number")
    val whitePlayer = varchar("white_player", 255)
    val blackPlayer = varchar("black_player", 255)
    val pgnHeader = text("pgn_header")
    val pgnMoves = text("pgn_moves").default("*")
    val result = text("result").default("*")

    override val primaryKey = PrimaryKey(gameId)
}