package com.example.tables

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
    val id = integer("id").autoIncrement()
    val firebaseUid = varchar("firebaseUid", length = 256)
    val nickname = varchar("nickname", length = 256).uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}