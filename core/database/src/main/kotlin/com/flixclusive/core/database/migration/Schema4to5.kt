package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date

class Schema4to5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val timeToday = Date().time

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `search_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `ownerId` INTEGER NOT NULL DEFAULT 1, 
                `query` TEXT NOT NULL, 
                `searchedOn` INTEGER NOT NULL DEFAULT $timeToday,
                UNIQUE(`query`, `ownerId`)
            )
        """.trimIndent())
    }
}