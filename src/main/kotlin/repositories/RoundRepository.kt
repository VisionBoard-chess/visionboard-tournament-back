package com.example.repositories

import com.example.tables.RoundTable
import com.example.models.Round
import kotlinx.coroutines.Dispatchers

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime

class RoundRepository {

    suspend fun createRound(round: Round): Round = dbQuery {
        RoundTable.insert {
            it[roundId] = round.roundId
            it[tournamentId] = round.tournamentId
            it[name] = round.name
            it[roundNumber] = round.roundNumber
            it[pgn] = round.pgn
            it[lichessRoundId] = round.lichessRoundId
            it[status] = round.status
            it[startDate] = round.startDate?.toString()
        }
        round
    }

    suspend fun existsByTournamentAndNumber(tournamentId: String, roundNumber: Int): Boolean = dbQuery {
        RoundTable.selectAll()
            .where { (RoundTable.tournamentId eq tournamentId) and (RoundTable.roundNumber eq roundNumber) }
            .count() > 0
    }
    suspend fun findByTournamentId(tournamentId: String): List<Round> = dbQuery {
        RoundTable.selectAll()
            .where { RoundTable.tournamentId eq tournamentId }
            .orderBy(RoundTable.roundNumber)
            .map { rowToRound(it) }
    }

    suspend fun findById(roundId: String): Round? = dbQuery {
        RoundTable.selectAll()
            .where { RoundTable.roundId eq roundId }
            .singleOrNull()
            ?.let { rowToRound(it) }
    }

    suspend fun updatePGN(roundId: String, pgn: String): Boolean = dbQuery {
        RoundTable.update({ RoundTable.roundId eq roundId }) {
            it[RoundTable.pgn] = pgn
        } > 0
    }

    private fun rowToRound(row: ResultRow): Round = Round(
        roundId = row[RoundTable.roundId],
        tournamentId = row[RoundTable.tournamentId],
        name = row[RoundTable.name],
        roundNumber = row[RoundTable.roundNumber],
        pgn = row[RoundTable.pgn],
        lichessRoundId = row[RoundTable.lichessRoundId],
        status = row[RoundTable.status],
        startDate = row[RoundTable.startDate]?.let { LocalDateTime.parse(it) }
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}