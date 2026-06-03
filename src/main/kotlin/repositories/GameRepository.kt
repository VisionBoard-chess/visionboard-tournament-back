package com.example.repositories

import com.example.models.Game
import com.example.models.GameResponse
import com.example.models.GameUpdateResponse
import com.example.models.IndividualGame
import com.example.models.IndividualGameRequest
import com.example.tables.GameTable
import com.example.tables.GameType
import com.example.tables.IndividualGameTable
import com.example.tables.RoundTable
import com.example.tables.TournamentGameTable
import com.example.tables.TournamentTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime

class GameRepository {
    suspend fun getGamesByRoundId(roundId: String): List<GameResponse> = dbQuery {
        (GameTable innerJoin TournamentGameTable).selectAll()
            .where { TournamentGameTable.roundId eq roundId }
            .map { rowToTournamentGame(it) }
    }

    suspend fun getGamesByAccessCode(accessCode: String): List<GameResponse> = dbQuery {
        val tournament = TournamentTable.selectAll()
            .where { TournamentTable.accessCode eq accessCode }
            .singleOrNull() ?: return@dbQuery emptyList()

        val activeRound = RoundTable.selectAll()
            .where { RoundTable.tournamentId eq tournament[TournamentTable.id] and (RoundTable.status eq "ACTIVE") }
            .singleOrNull() ?: return@dbQuery emptyList()

        (GameTable innerJoin TournamentGameTable).selectAll()
            .where { TournamentGameTable.roundId eq activeRound[RoundTable.roundId] }
            .map { rowToTournamentGame(it) }
    }

    suspend fun getTournamentGameById(gameId: String): GameResponse? = dbQuery {
        (GameTable innerJoin TournamentGameTable).selectAll()
            .where { GameTable.gameId eq gameId }
            .map{ rowToTournamentGame(it) }
            .singleOrNull()
    }

    suspend fun getGameByIdServerResponse(gameId: String): GameUpdateResponse? = dbQuery{
        (GameTable innerJoin TournamentGameTable).selectAll()
            .where { GameTable.gameId eq gameId }
            .map {
                GameUpdateResponse(
                    id = it[GameTable.gameId],
                    roundId = it[TournamentGameTable.roundId],
                    pgn = "${it[GameTable.pgnHeader]}\n\n${it[GameTable.pgnMoves]}"
                )
            }
            .singleOrNull()
    }

    suspend fun createTournamentGame(game: Game): GameResponse = dbQuery {
        GameTable.insert {
            it[gameId] = game.id
            it[whitePlayer] = game.whiteName
            it[blackPlayer] = game.blackName
            it[pgnHeader] = game.pgnHeader
            it[pgnMoves] = game.pgnMoves
            it[result] = game.result
            it[gameType] = GameType.TOURNAMENT
        }
        TournamentGameTable.insert {
            it[gameId] = game.id
            it[roundId] = game.roundId
            it[tableNumber] = game.tableNumber
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

    suspend fun updateResult(gameId: String, result: String): Boolean = dbQuery {
        GameTable.update({ GameTable.gameId eq gameId }) {
            it[GameTable.result] = result
        } > 0
    }


    suspend fun createIndividualGame(id: String, game: IndividualGameRequest): IndividualGame = dbQuery {
        GameTable.insert{
            it[gameId] = id
            it[whitePlayer] = game.whiteName
            it[blackPlayer] = game.blackName
            it[pgnHeader] = game.pgnHeader
            it[pgnMoves] = game.pgnMoves
            it[result] = game.result
            it[gameType] = GameType.INDIVIDUAL
        }
        IndividualGameTable.insert {
            it[gameId] = id
            it[creatorId] = game.creatorId
            it[participantId] = game.participantId
            it[createdAt] = LocalDateTime.now()
        }
        //tal vez sea mejor poner gameResponse?
//        GameResponse(
//            id          = game.id,
//            tableNumber = null,
//            white       = game.whiteName,
//            black       = game.blackName,
//            pgn         = "${game.pgnHeader}\n\n${game.pgnMoves}",
//            result      = game.result
//        )
        IndividualGame(
            id = id,
            white = game.whiteName,
            black = game.blackName,
            pgn = "${game.pgnHeader}\n\n${game.pgnMoves}",
            result = game.result,
            createdAt = IndividualGameTable.selectAll().where { IndividualGameTable.gameId eq id }.single()[IndividualGameTable.createdAt].toString()
        )

    }

    suspend fun getIndividualGamesByPlayer(playerId: Int): List<IndividualGame> = dbQuery {
        (GameTable innerJoin IndividualGameTable).selectAll()
            .where { (IndividualGameTable.creatorId eq playerId) or (IndividualGameTable.participantId eq playerId) }
            .orderBy(IndividualGameTable.createdAt, SortOrder.DESC)
            .map { rowToIndividualGame(it) }
    }

    suspend fun updatePGNMoves(gameId: String, pgn: String): Boolean = dbQuery {
        //pgn.removeSuffix("*") delete * final del pgn
        GameTable.update({ GameTable.gameId eq gameId }) {
            it[GameTable.pgnMoves] = pgn
        } > 0
    }

    private fun rowToIndividualGame(row: ResultRow): IndividualGame = IndividualGame(
        id = row[GameTable.gameId],
        white = row[GameTable.whitePlayer],
        black = row[GameTable.blackPlayer],
        pgn = "${row[GameTable.pgnHeader]}\n\n${row[GameTable.pgnMoves]}",
        result = row[GameTable.result],
        createdAt = row[IndividualGameTable.createdAt].toString()
    )

    private fun rowToTournamentGame(row: ResultRow): GameResponse = GameResponse(
        id = row[GameTable.gameId],
        tableNumber = row[TournamentGameTable.tableNumber],
        white = row[GameTable.whitePlayer],
        black = row[GameTable.blackPlayer],
        pgn = "${row[GameTable.pgnHeader]}\n\n${row[GameTable.pgnMoves]}",
        result = row[GameTable.result]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}