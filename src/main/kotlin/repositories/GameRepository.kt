package com.example.repositories

import com.example.models.Game
import com.example.models.GameResponse
import com.example.models.GameUpdateResponse
import com.example.tables.GameTable
import com.example.tables.RoundTable
import com.example.tables.TournamentTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class GameRepository {
    suspend fun getGamesByRoundId(roundId: String): List<GameResponse> = dbQuery {
        GameTable.selectAll()
            .where { GameTable.roundId eq roundId }
            .map { rowToGame(it) }
    }

    suspend fun getGamesByAccessCode(accessCode: String): List<GameResponse> = dbQuery {
        val tournament = TournamentTable.selectAll()
            .where { TournamentTable.accessCode eq accessCode }
            .singleOrNull() ?: return@dbQuery emptyList()

        val activeRound = RoundTable.selectAll()
            .where { RoundTable.tournamentId eq tournament[TournamentTable.id] and (RoundTable.status eq "ACTIVE") }
            .singleOrNull() ?: return@dbQuery emptyList()

        GameTable.selectAll()
            .where { GameTable.roundId eq activeRound[RoundTable.roundId] }
            .map { rowToGame(it) }
    }

    suspend fun getGameById(gameId: String): GameResponse? = dbQuery {
        GameTable.selectAll()
            .where { GameTable.gameId eq gameId }
            .map{ rowToGame(it) }
            .singleOrNull()
    }

    suspend fun getGameByIdServerResponse(gameId: String): GameUpdateResponse? = dbQuery{
        GameTable.selectAll()
            .where { GameTable.gameId eq gameId }
            .map {
                GameUpdateResponse(
                    id = it[GameTable.gameId],
                    roundId = it[GameTable.roundId],
                    pgn = "${it[GameTable.pgnHeader]}\n\n${it[GameTable.pgnMoves]}"
                )
            }
            .singleOrNull()
    }

    suspend fun createGame(game: Game): GameResponse = dbQuery {
        GameTable.insert {
            it[gameId] = game.id
            it[roundId] = game.roundId
            it[tableNumber] = game.tableNumber
            it[whitePlayer] = game.whiteName
            it[blackPlayer] = game.blackName
            it[pgnHeader] = game.pgnHeader
            it[pgnMoves] = game.pgnMoves
            it[result] = game.result
        }
        GameResponse(
            id = game.id,
            tableNumber = game.tableNumber,
            white = game.whiteName,
            black = game.blackName,
            pgn = "${game.pgnHeader}\n\n${game.pgnMoves}",
            result = game.result
        )
    }

    suspend fun updatePGNMoves(gameId: String, pgn: String): Boolean = dbQuery {
        //pgn.removeSuffix("*") delete * final del pgn
        GameTable.update({ GameTable.gameId eq gameId }) {
            it[GameTable.pgnMoves] = pgn
        } > 0
    }

    private fun rowToGame(row: ResultRow): GameResponse = GameResponse(
        id = row[GameTable.gameId],
        tableNumber = row[GameTable.tableNumber],
        white = row[GameTable.whitePlayer],
        black = row[GameTable.blackPlayer],
        pgn = "${row[GameTable.pgnHeader]}\n\n${row[GameTable.pgnMoves]}",
        result = row[GameTable.result]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}