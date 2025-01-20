package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Schema7to8 : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
        """
                CREATE TABLE IF NOT EXISTS library_lists (
                    listId TEXT NOT NULL PRIMARY KEY,
                    ownerId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """,
        )

        db.execSQL(
        """
                CREATE TABLE IF NOT EXISTS library_list_entries (
                    entryId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    listId TEXT NOT NULL,
                    itemId TEXT NOT NULL,
                    addedAt INTEGER NOT NULL,
                    FOREIGN KEY (listId) REFERENCES library_lists(listId) ON DELETE CASCADE
                )
            """,
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_custom_library_list_entries_listId ON library_list_entries(listId)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_custom_library_list_entries_itemId_listId ON library_list_entries(itemId, listId)",
        )
    }
}
