package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal class Schema1to2 : Migration(startVersion = 1, endVersion = 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add watchlist table
        db.execSQL("CREATE TABLE IF NOT EXISTS `watchlist` (`ownerId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY NOT NULL, `film` TEXT NOT NULL)")
        // Add user table
        db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`userId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `image` INTEGER NOT NULL)")
        // Edit watch history table to add ownerId
        db.execSQL("ALTER TABLE `watch_history` ADD COLUMN ownerId INTEGER NOT NULL DEFAULT 1")
    }
}