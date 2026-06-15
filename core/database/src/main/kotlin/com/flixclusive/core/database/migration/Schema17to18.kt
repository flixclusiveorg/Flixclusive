package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date

internal object Schema17to18 : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        seedRepositoryWithDefaultProviderRepo(db)
        migrateCacheLinksTable(db)
    }

    fun seedRepositoryWithDefaultProviderRepo(db: SupportSQLiteDatabase) {
        val now = Date().time
        db.execSQL(
            """
                INSERT OR IGNORE INTO `repositories` (
                    url, userId, owner, name, rawLinkFormat, createdAt, updatedAt
                ) SELECT
                    'https://github.com/flixclusiveorg/flx-providers',
                    userId,
                    'flixclusiveorg',
                    'flx-providers',
                    'https://raw.githubusercontent.com/flixclusiveorg/flx-providers/%branch%/%filename%',
                    $now,
                    $now
                FROM User
            """.trimIndent()
        )

        db.execSQL(
            """
                INSERT OR IGNORE INTO `repositories` (
                    url, userId, owner, name, rawLinkFormat, createdAt, updatedAt
                ) SELECT
                    'https://github.com/flixclusiveorg/provider-template',
                    userId,
                    'flixclusiveorg',
                    'provider-template',
                    'https://raw.githubusercontent.com/flixclusiveorg/provider-template/%branch%/%filename%',
                    $now,
                    $now
                FROM User
            """.trimIndent()
        )
    }

    private fun migrateCacheLinksTable(db: SupportSQLiteDatabase) {
        // Drop child tables first to respect FK ordering
        db.execSQL("DROP TABLE IF EXISTS `cached_streams`")
        db.execSQL("DROP TABLE IF EXISTS `cached_subtitles`")
        db.execSQL("DROP TABLE IF EXISTS `cached_media_links`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_media_links` (
                `id`            TEXT    NOT NULL PRIMARY KEY,
                `providerId`    TEXT    NOT NULL,
                `ownerId`       TEXT    NOT NULL,
                `mediaId`       TEXT    NOT NULL,
                `episodeNumber` INTEGER,
                `seasonNumber`  INTEGER,
                `thumbnail`     TEXT,
                `createdAt`     INTEGER NOT NULL,
                `updatedAt`     INTEGER NOT NULL,
                FOREIGN KEY (`providerId`, `ownerId`)
                    REFERENCES `installed_providers` (`id`, `ownerId`)
                    ON DELETE CASCADE ON UPDATE CASCADE,
                FOREIGN KEY (`mediaId`)
                    REFERENCES `media` (`id`)
                    ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_media_links_ownerId` ON `cached_media_links` (`ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_media_links_mediaId` ON `cached_media_links` (`mediaId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_media_links_providerId_ownerId` ON `cached_media_links` (`providerId`, `ownerId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_cached_media_links_composite` ON `cached_media_links` (`ownerId`, `mediaId`, `episodeNumber`, `seasonNumber`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_streams` (
                `url`                   TEXT    NOT NULL,
                `parentId`              TEXT    NOT NULL,
                `label`                 TEXT    NOT NULL,
                `description`           TEXT,
                `expiresOn`             INTEGER,
                `customHeaders`         TEXT,
                `isThirdPartyGateway`   INTEGER NOT NULL DEFAULT 0,
                `thirdPartyGatewayName` TEXT,
                `thirdPartyGatewayLogo` TEXT,
                `isDead`                INTEGER NOT NULL DEFAULT 0,
                `createdAt`             INTEGER NOT NULL,
                `updatedAt`             INTEGER NOT NULL,
                PRIMARY KEY (`url`, `parentId`),
                FOREIGN KEY (`parentId`)
                    REFERENCES `cached_media_links` (`id`)
                    ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_streams_parentId` ON `cached_streams` (`parentId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_subtitles` (
                `url`                 TEXT    NOT NULL,
                `parentId`            TEXT    NOT NULL,
                `language`            TEXT    NOT NULL,
                `description`         TEXT,
                `subtitleSource`      TEXT    NOT NULL DEFAULT 'ONLINE',
                `customHeaders`       TEXT,
                `isDead`              INTEGER NOT NULL DEFAULT 0,
                `createdAt`           INTEGER NOT NULL,
                `updatedAt`           INTEGER NOT NULL,
                PRIMARY KEY (`url`, `parentId`),
                FOREIGN KEY (`parentId`)
                    REFERENCES `cached_media_links` (`id`)
                    ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_subtitles_parentId` ON `cached_subtitles` (`parentId`)")
    }
}
