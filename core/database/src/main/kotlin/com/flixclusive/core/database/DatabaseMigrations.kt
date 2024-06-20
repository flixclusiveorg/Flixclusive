package com.flixclusive.core.database

import android.os.Build
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flixclusive.core.util.exception.safeCall
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
            safeCall {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    db.updateIdColumnToStringDataType(tableName = "watchlist")
                    db.updateIdColumnToStringDataType(tableName = "watch_history")
                    return
                }
            }

            db.updateWatchlistTable()
            db.updateWatchHistoryTable()
        }

        private fun SupportSQLiteDatabase.updateIdColumnToStringDataType(tableName: String) {
            execSQL(
                """
                ALTER TABLE $tableName ADD COLUMN _id TEXT
                UPDATE $tableName SET _id = CAST(id as TEXT)
                ALTER TABLE $tableName DROP COLUMN id
                ALTER TABLE $tableName RENAME COLUMN _id TO id
            """.trimIndent()
            )
            return
        }

        private fun SupportSQLiteDatabase.updateWatchlistTable() {
            val tableName = "watchlist"
            execSQL(
                """
                CREATE TABLE ${tableName}_new (
                    id TEXT PRIMARY KEY NOT NULL,
                    ownerId INTEGER NOT NULL,
                    addedOn INTEGER NOT NULL,
                    film TEXT NOT NULL
                );
             """.trimIndent())


            execSQL("INSERT INTO ${tableName}_new SELECT CAST(id AS TEXT), ownerId, addedOn, film FROM $tableName;")

            execSQL("DROP TABLE $tableName;")
            execSQL("ALTER TABLE ${tableName}_new RENAME TO $tableName;")
        }

        private fun SupportSQLiteDatabase.updateWatchHistoryTable() {
            val tableName = "watch_history"
            execSQL(
                """
                CREATE TABLE ${tableName}_new (
                    id TEXT PRIMARY KEY NOT NULL,
                    ownerId INTEGER NOT NULL,
                    seasons INTEGER,
                    episodes TEXT NOT NULL,
                    episodesWatched TEXT NOT NULL,
                    dateWatched INTEGER NOT NULL,
                    film TEXT NOT NULL
                );
             """.trimIndent())

            execSQL("INSERT INTO ${tableName}_new SELECT CAST(id AS TEXT), ownerId, seasons, episodes, episodesWatched, dateWatched, film FROM $tableName;")

            execSQL("DROP TABLE $tableName;")
            execSQL("ALTER TABLE ${tableName}_new RENAME TO $tableName;")
        }
    }
}