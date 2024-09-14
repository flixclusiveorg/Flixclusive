package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date

internal class Schema2to3 : Migration(startVersion = 2, endVersion = 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val timeToday = Date().time

        // Edit watchlist table to add dateAdded column
        db.execSQL("ALTER TABLE `watchlist` ADD COLUMN addedOn INTEGER NOT NULL DEFAULT $timeToday")
    }
}