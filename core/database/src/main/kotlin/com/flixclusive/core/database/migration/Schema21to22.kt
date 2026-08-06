package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Schema21to22 : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `download_items` ADD COLUMN `downloadBytesPerSecond` INTEGER NOT NULL DEFAULT 0",
        )
    }
}
