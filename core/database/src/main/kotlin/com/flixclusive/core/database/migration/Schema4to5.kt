package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal class Schema4to5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `search_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `ownerId` INTEGER NOT NULL, 
                `query` TEXT NOT NULL, 
                `searchedOn` INTEGER NOT NULL,
                UNIQUE(`query`, `ownerId`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query_ownerId` ON `search_history` (`query`, `ownerId`)
        """)
    }
}