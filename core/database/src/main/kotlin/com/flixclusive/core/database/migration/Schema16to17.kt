package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Schema16to17 : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateInstalledProviders(db)
    }

    private fun migrateInstalledProviders(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `index_installed_providers_repositoryUrl`")
        db.execSQL("DROP INDEX IF EXISTS `index_installed_providers_ownerId`")
        db.execSQL("DROP INDEX IF EXISTS `index_installed_providers_repositoryUrl_ownerId`")
        db.execSQL("DROP INDEX IF EXISTS `index_installed_providers_sortOrder`")
        db.execSQL("DROP INDEX IF EXISTS `index_installed_providers_isEnabled`")

        db.execSQL("ALTER TABLE `installed_providers` RENAME TO `installed_providers_old`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `installed_providers` (
                `id`                  TEXT    NOT NULL,
                `ownerId`             TEXT    NOT NULL,
                `repositoryUrl`       TEXT    NOT NULL,
                `filePath`            TEXT    NOT NULL,
                `isCatalogEnabled`    INTEGER NOT NULL,
                `isCrossMatchEnabled` INTEGER NOT NULL,
                `isMediaLinkEnabled`  INTEGER NOT NULL,
                `isMetadataEnabled`   INTEGER NOT NULL,
                `isSearchEnabled`     INTEGER NOT NULL,
                `isTrackerEnabled`    INTEGER NOT NULL,
                `isDebug`             INTEGER NOT NULL,
                `createdAt`           INTEGER NOT NULL,
                `updatedAt`           INTEGER NOT NULL,
                PRIMARY KEY (`id`, `ownerId`),
                FOREIGN KEY (`repositoryUrl`, `ownerId`)
                    REFERENCES `repositories` (`url`, `userId`)
                    ON DELETE CASCADE
                    ON UPDATE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `installed_providers` (
                `id`, `ownerId`, `repositoryUrl`, `filePath`,
                `isCatalogEnabled`, `isCrossMatchEnabled`, `isMediaLinkEnabled`,
                `isMetadataEnabled`, `isSearchEnabled`, `isTrackerEnabled`,
                `isDebug`, `createdAt`, `updatedAt`
            )
            SELECT
                `id`, `ownerId`, `repositoryUrl`, `filePath`,
                `isCatalogEnabled`, `isCrossMatchEnabled`, `isMediaLinkEnabled`,
                `isMetadataEnabled`, `isSearchEnabled`, `isTrackerEnabled`,
                `isDebug`, `createdAt`, `updatedAt`
            FROM `installed_providers_old`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `installed_providers_old`")

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_repositoryUrl` ON `installed_providers` (`repositoryUrl`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_ownerId` ON `installed_providers` (`ownerId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_repositoryUrl_ownerId` ON `installed_providers` (`repositoryUrl`, `ownerId`)"
        )
    }
}
