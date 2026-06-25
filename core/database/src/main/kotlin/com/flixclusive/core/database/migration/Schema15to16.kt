package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Schema15to16 : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        addCapabilityColumns(db)
    }

    private fun addCapabilityColumns(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `installed_providers` ADD COLUMN `isCatalogEnabled` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `installed_providers` ADD COLUMN `isCrossMatchEnabled` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `installed_providers` ADD COLUMN `isMediaLinkEnabled` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `installed_providers` ADD COLUMN `isMetadataEnabled` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `installed_providers` ADD COLUMN `isSearchEnabled` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `installed_providers` ADD COLUMN `isTrackerEnabled` INTEGER NOT NULL DEFAULT 1")
    }
}
