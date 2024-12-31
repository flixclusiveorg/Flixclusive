package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal class Schema6to7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(updateDefaultOwnerId("watchlist"))
        db.migrateWatchlist()

        db.execSQL(updateDefaultOwnerId("watch_history"))
        db.migrateWatchHistory()

        db.execSQL(updateDefaultOwnerId("search_history"))
        db.migrateSearchHistory()

    }

    private fun updateDefaultOwnerId(table: String): String {
        return """
            UPDATE $table
            SET ownerId = 0
            WHERE ownerId = 1 AND NOT EXISTS (
                SELECT 1 FROM User WHERE userId = 0
            ) AND (SELECT COUNT(*) FROM User) = 1;
        """.trimIndent()
    }

    private fun SupportSQLiteDatabase.migrateSearchHistory() {
        execSQL("""
            CREATE TABLE IF NOT EXISTS search_history_temp (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                query TEXT NOT NULL,
                ownerId INTEGER NOT NULL,
                searchedOn INTEGER NOT NULL
            )
        """)

        execSQL("""
            INSERT INTO search_history_temp (id, query, ownerId, searchedOn)
            SELECT id, query, ownerId, searchedOn FROM search_history
        """)

        execSQL("DROP TABLE search_history")
        execSQL("ALTER TABLE search_history_temp RENAME TO search_history")

        execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query_ownerId` ON `search_history` (`query`, `ownerId`)
        """)
    }

    private fun SupportSQLiteDatabase.migrateWatchHistory() {
        execSQL("""
            CREATE TABLE IF NOT EXISTS watch_history_temp (
                id TEXT PRIMARY KEY NOT NULL,
                ownerId INTEGER NOT NULL,
                film TEXT NOT NULL,
                seasons INTEGER,
                episodes TEXT NOT NULL,
                episodesWatched TEXT NOT NULL,
                dateWatched INTEGER NOT NULL
            )
        """)

        execSQL("""
            INSERT INTO watch_history_temp (
                id, 
                ownerId, 
                film, 
                seasons, 
                episodes, 
                episodesWatched, 
                dateWatched
            )
            SELECT 
                id, 
                ownerId, 
                film, 
                seasons, 
                episodes, 
                episodesWatched, 
                dateWatched 
            FROM watch_history
        """)

        execSQL("DROP TABLE watch_history")
        execSQL("ALTER TABLE watch_history_temp RENAME TO watch_history")
    }

    private fun SupportSQLiteDatabase.migrateWatchlist() {
        execSQL("""
            CREATE TABLE IF NOT EXISTS watchlist_temp (
                id TEXT PRIMARY KEY NOT NULL,
                ownerId INTEGER NOT NULL,
                film TEXT NOT NULL,
                addedOn INTEGER NOT NULL
            )
        """)

        execSQL("""
            INSERT INTO watchlist_temp (
                id, 
                ownerId, 
                film, 
                addedOn
            )
            SELECT 
                id, 
                ownerId, 
                film, 
                addedOn
            FROM watchlist
        """)

        execSQL("DROP TABLE watchlist")
        execSQL("ALTER TABLE watchlist_temp RENAME TO watchlist")
    }
}