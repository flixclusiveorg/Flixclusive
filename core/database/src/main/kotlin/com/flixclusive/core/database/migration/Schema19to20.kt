package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Schema19to20 : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Recreate cached_streams without CASCADE
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_streams_new` (
                `url` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `description` TEXT,
                `customHeaders` TEXT,
                `isDead` INTEGER NOT NULL,
                `providerId` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `mediaId` TEXT NOT NULL,
                `episodeNumber` INTEGER,
                `seasonNumber` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `expiresOn` INTEGER,
                `isThirdPartyGateway` INTEGER NOT NULL,
                `thirdPartyGatewayName` TEXT,
                `thirdPartyGatewayLogo` TEXT,
                PRIMARY KEY(`url`),
                FOREIGN KEY(`mediaId`) REFERENCES `media`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO `cached_streams_new` SELECT * FROM `cached_streams`")
        db.execSQL("DROP TABLE `cached_streams`")
        db.execSQL("ALTER TABLE `cached_streams_new` RENAME TO `cached_streams`")

        // Re-create indices for cached_streams
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_streams_ownerId` ON `cached_streams` (`ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_streams_mediaId` ON `cached_streams` (`mediaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_streams_providerId` ON `cached_streams` (`providerId`)")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_cached_streams_mediaId_seasonNumber_episodeNumber` ON `cached_streams` (`mediaId`, `seasonNumber`, `episodeNumber`)
            """.trimIndent()
        )

        // 2. Recreate cached_subtitles without CASCADE
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_subtitles_new` (
                `url` TEXT NOT NULL,
                `language` TEXT NOT NULL,
                `description` TEXT,
                `customHeaders` TEXT,
                `isDead` INTEGER NOT NULL,
                `providerId` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `mediaId` TEXT NOT NULL,
                `episodeNumber` INTEGER,
                `seasonNumber` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `subtitleSource` TEXT NOT NULL,
                PRIMARY KEY(`url`),
                FOREIGN KEY(`mediaId`) REFERENCES `media`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO `cached_subtitles_new` SELECT * FROM `cached_subtitles`")
        db.execSQL("DROP TABLE `cached_subtitles`")
        db.execSQL("ALTER TABLE `cached_subtitles_new` RENAME TO `cached_subtitles`")

        // Re-create indices for cached_subtitles
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_subtitles_ownerId` ON `cached_subtitles` (`ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_subtitles_mediaId` ON `cached_subtitles` (`mediaId`)")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_cached_subtitles_providerId` ON `cached_subtitles` (`providerId`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_cached_subtitles_mediaId_seasonNumber_episodeNumber` ON `cached_subtitles` (`mediaId`, `seasonNumber`, `episodeNumber`)
            """.trimIndent()
        )
    }
}
