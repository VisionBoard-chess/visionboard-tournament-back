package com.example.repositories

import com.example.models.Tournament
import com.example.tables.TournamentTable

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


class TournamentRepository {
    suspend fun createTournament(tournament: Tournament): Tournament = dbQuery {
        TournamentTable.insert {
            it[id] = tournament.tournamentId
            it[name] = tournament.name
            it[description] = tournament.description
            it[typeOf] = tournament.typeOf
            it[startDate] = tournament.startDate
            it[creatorId] = tournament.creatorId
            it[accessCode] = tournament.accessCode
            it[lichessBroadcastId] = tournament.lichessBroadcastId
            it[status] = tournament.status
    }
        tournament
    }

    suspend fun getAll(): List<Tournament> = dbQuery {
        TournamentTable.selectAll()
            .map { rowToTournament(it) }
    }

    suspend fun findByCreatorId(creatorId: Int): List<Tournament> = dbQuery {
        TournamentTable.selectAll().where { TournamentTable.creatorId eq creatorId }
            .map { rowToTournament(it) }
    }

    suspend fun findById(id: String): Tournament? = dbQuery {
        TournamentTable.selectAll().where { TournamentTable.id eq id }
            .mapNotNull { rowToTournament(it) }
            .singleOrNull()
    }

    suspend fun deleteTournament(id: String): Boolean = dbQuery {
        TournamentTable.deleteWhere { TournamentTable.id eq id } > 0
    }

    private fun rowToTournament(row: ResultRow): Tournament =
        Tournament(
            tournamentId = row[TournamentTable.id],
            name = row[TournamentTable.name],
            description = row[TournamentTable.description],
            typeOf = row[TournamentTable.typeOf],
            startDate = row[TournamentTable.startDate],
            creatorId = row[TournamentTable.creatorId],
            accessCode = row[TournamentTable.accessCode],
            lichessBroadcastId = row[TournamentTable.lichessBroadcastId],
            status = row[TournamentTable.status]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}