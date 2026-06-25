package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Schema18to19 : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create new tables
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_streams_new` (
                `url` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `description` TEXT,
                `customHeaders` TEXT,
                `isDead` INTEGER NOT NULL DEFAULT 0,
                `providerId` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `mediaId` TEXT NOT NULL,
                `episodeNumber` INTEGER,
                `seasonNumber` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `expiresOn` INTEGER,
                `isThirdPartyGateway` INTEGER NOT NULL DEFAULT 0,
                `thirdPartyGatewayName` TEXT,
                `thirdPartyGatewayLogo` TEXT,
                PRIMARY KEY(`url`),
                FOREIGN KEY(`mediaId`) REFERENCES `media`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_subtitles_new` (
                `url` TEXT NOT NULL,
                `language` TEXT NOT NULL,
                `description` TEXT,
                `customHeaders` TEXT,
                `isDead` INTEGER NOT NULL DEFAULT 0,
                `providerId` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `mediaId` TEXT NOT NULL,
                `episodeNumber` INTEGER,
                `seasonNumber` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `subtitleSource` TEXT NOT NULL DEFAULT 'ONLINE',
                PRIMARY KEY(`url`),
                FOREIGN KEY(`mediaId`) REFERENCES `media`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // 2. Migrate data
        db.execSQL(
            """
            INSERT OR IGNORE INTO `cached_streams_new` (
                url, label, description, customHeaders, isDead,
                providerId, ownerId, mediaId, episodeNumber, seasonNumber,
                createdAt, updatedAt, expiresOn, isThirdPartyGateway,
                thirdPartyGatewayName, thirdPartyGatewayLogo
            )
            SELECT s.url, s.label, s.description, s.customHeaders, s.isDead,
                   m.providerId, m.ownerId, m.mediaId, m.episodeNumber, m.seasonNumber,
                   s.createdAt, s.updatedAt, s.expiresOn, s.isThirdPartyGateway,
                   s.thirdPartyGatewayName, s.thirdPartyGatewayLogo
            FROM cached_streams s
            JOIN cached_media_links m ON s.parentId = m.id
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO `cached_subtitles_new` (
                url, language, description, customHeaders, isDead,
                providerId, ownerId, mediaId, episodeNumber, seasonNumber,
                createdAt, updatedAt, subtitleSource
            )
            SELECT s.url, s.language, s.description, s.customHeaders, s.isDead,
                   m.providerId, m.ownerId, m.mediaId, m.episodeNumber, m.seasonNumber,
                   s.createdAt, s.updatedAt, s.subtitleSource
            FROM cached_subtitles s
            JOIN cached_media_links m ON s.parentId = m.id
            """.trimIndent()
        )

        // 3. Drop old tables and rename new ones
        db.execSQL("DROP TABLE `cached_streams`")
        db.execSQL("DROP TABLE `cached_subtitles`")
        db.execSQL("DROP TABLE `cached_media_links`")

        db.execSQL("ALTER TABLE `cached_streams_new` RENAME TO `cached_streams`")
        db.execSQL("ALTER TABLE `cached_subtitles_new` RENAME TO `cached_subtitles`")

        // 4. Create indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_streams_ownerId` ON `cached_streams` (`ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_streams_mediaId` ON `cached_streams` (`mediaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_streams_providerId` ON `cached_streams` (`providerId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_streams_composite` ON `cached_streams` (`mediaId`, `seasonNumber`, `episodeNumber`)"
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_subtitles_ownerId` ON `cached_subtitles` (`ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_subtitles_mediaId` ON `cached_subtitles` (`mediaId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_subtitles_providerId` ON `cached_subtitles` (`providerId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_subtitles_composite` ON `cached_subtitles` (`mediaId`, `seasonNumber`, `episodeNumber`)"
        )
    }
}
