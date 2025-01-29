package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Schema7to8 : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_lists` (
                `listId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_list_items` (
                `itemId` TEXT NOT NULL,
                `film` TEXT NOT NULL,
                PRIMARY KEY(`itemId`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_list_and_item_cross_ref` (
                `addedAt` INTEGER NOT NULL,
                `listId` INTEGER NOT NULL,
                `itemId` TEXT NOT NULL,
                PRIMARY KEY(`listId`, `itemId`),
                FOREIGN KEY(`itemId`) REFERENCES `library_list_items`(`itemId`) ON DELETE RESTRICT,
                FOREIGN KEY(`listId`) REFERENCES `library_lists`(`listId`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}
