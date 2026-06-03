package com.example.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object GameTable : Table("games") {
    val gameId = varchar("game_id", 36)
    val whitePlayer = varchar("white_player", 255)
    val blackPlayer = varchar("black_player", 255)
    val pgnHeader = text("pgn_header")
    val pgnMoves = text("pgn_moves").default("*")
    val result = text("result").default("*")
    val gameType = enumerationByName("game_type", 20, GameType::class)

    override val primaryKey = PrimaryKey(gameId)
}

enum class GameType{TOURNAMENT, INDIVIDUAL}

object TournamentGameTable : Table("tournament_game") {
    val gameId = varchar("game_id", 36).references(GameTable.gameId)
    val roundId = varchar("round_id", 36).references(RoundTable.roundId)
    val tableNumber = integer("table_number")

    override val primaryKey = PrimaryKey(gameId)
}

object IndividualGameTable : Table("individual_game") {
    val gameId = varchar("game_id", 36).references(GameTable.gameId)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val creatorId = integer("creator_id").references(UserTable.id)
    val participantId = integer("participant_id").references(UserTable.id).nullable()

    override val primaryKey = PrimaryKey(gameId)
}