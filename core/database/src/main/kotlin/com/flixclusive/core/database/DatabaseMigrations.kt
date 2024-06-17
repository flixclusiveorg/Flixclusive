package com.flixclusive.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date

internal object DatabaseMigrations {
    class Schema1to2 : Migration(startVersion = 1, endVersion = 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add watchlist table
            db.execSQL("CREATE TABLE IF NOT EXISTS `watchlist` (`ownerId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY NOT NULL, `film` TEXT NOT NULL)")
            // Add user table
            db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`userId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `image` INTEGER NOT NULL)")
            // Edit watch history table to add ownerId
            db.execSQL("ALTER TABLE `watch_history` ADD COLUMN ownerId INTEGER NOT NULL DEFAULT 1")
        }
    }

    class Schema2to3 : Migration(startVersion = 2, endVersion = 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val timeToday = Date().time

            // Edit watchlist table to add dateAdded column
            db.execSQL("ALTER TABLE `watchlist` ADD COLUMN addedOn INTEGER NOT NULL DEFAULT $timeToday")
        }
    }

    class Schema3to4 : Migration(startVersion = 3, endVersion = 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Edit `id` column to be string data type
            db.updateIdColumnToStringDataType("`watchlist`")
            db.updateIdColumnToStringDataType("`watch_history`")
        }

        private fun SupportSQLiteDatabase.updateIdColumnToStringDataType(tableName: String) {
            execSQL("ALTER TABLE $tableName ADD COLUMN _id TEXT")
            execSQL("UPDATE $tableName SET _id = CAST(id as TEXT)")
            execSQL("ALTER TABLE $tableName DROP COLUMN id")
            execSQL("ALTER TABLE $tableName RENAME COLUMN _id TO id")
        }
    }
}