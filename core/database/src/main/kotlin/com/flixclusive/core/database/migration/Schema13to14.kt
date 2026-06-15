package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Schema13to14 : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        renameFilmsTable(db)
        renameExternalIdsTable(db)
        renameMediaFtsTable(db)
        renameColumnForLibraryListItems(db)

        migrateMovieWatchHistory(db)
        migrateSeriesWatchHistory(db)
    }

    private fun renameFilmsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `media` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `providerId` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `posterImage` TEXT,
                `language` TEXT,
                `adult` INTEGER NOT NULL,
                `overview` TEXT,
                `rating` REAL,
                `backdropImage` TEXT,
                `releaseDate` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `media` (
                `id`, `providerId`, `type`, `title`, `posterImage`, `language`,
                `adult`, `overview`, `rating`, `backdropImage`, `releaseDate`,
                `createdAt`, `updatedAt`
            )
            SELECT `id`, `providerId`, `filmType`, `title`, `posterImage`, `language`,
                `adult`, `overview`, `rating`, `backdropImage`, `releaseDate`,
                `createdAt`, `updatedAt`
            FROM `films`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `films`")
        db.execSQL(
            """
            UPDATE media
            SET type = 'SHOW'
            WHERE type = 'TV_SHOW'
            """.trimIndent()
        )
    }

    private fun renameExternalIdsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `media_external_ids` (
                mediaId TEXT NOT NULL,
                providerId TEXT NOT NULL,
                source TEXT NOT NULL,
                externalId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY (mediaId, providerId, source),
                FOREIGN KEY (mediaId) REFERENCES media(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `media_external_ids` (
                mediaId, providerId, source, externalId, createdAt, updatedAt
            )
            SELECT filmId, providerId, source, externalId, createdAt, updatedAt
            FROM `film_external_ids`
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_media_external_ids_mediaId`
            ON `media_external_ids` (`mediaId`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_media_external_ids_providerId`
            ON `media_external_ids` (`providerId`)
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `film_external_ids`")
    }

    private fun renameMediaFtsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS `media_fts`
            USING fts3(
                `mediaId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `overview` TEXT NOT NULL
            )
        """
        )

        db.execSQL(
            """
            INSERT INTO media_fts (mediaId, title, overview)
            SELECT id, title, COALESCE(overview, '') FROM media
        """
        )

        db.execSQL("DROP TABLE `films_fts`")
    }

    private fun renameColumnForLibraryListItems(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_list_items_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `mediaId` TEXT NOT NULL,
                `listId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`listId`) REFERENCES `library_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`mediaId`) REFERENCES `media`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `library_list_items_new` (`id`, `mediaId`, `listId`, `createdAt`, `updatedAt`)
            SELECT `id`, `filmId`, `listId`, `createdAt`, `updatedAt`
            FROM `library_list_items`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `library_list_items`")
        db.execSQL("ALTER TABLE `library_list_items_new` RENAME TO `library_list_items`")

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_library_list_items_mediaId_listId` ON `library_list_items` (`mediaId`, `listId`)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_list_items_mediaId` ON `library_list_items` (`mediaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_list_items_listId` ON `library_list_items` (`listId`)")

        db.execSQL("DROP VIEW IF EXISTS `library_list_item_with_metadata`")
        db.execSQL(
            "CREATE VIEW `library_list_item_with_metadata` AS SELECT library_list_items.id AS item_id, library_list_items.mediaId AS item_mediaId, library_list_items.listId AS item_listId, library_list_items.createdAt AS item_createdAt, library_list_items.updatedAt AS item_updatedAt, media.id AS media_id, media.title AS media_title, media.providerId AS media_providerId, media.type AS media_type, media.overview AS media_overview, media.posterImage AS media_posterImage, media.adult AS media_adult, media.language AS media_language, media.rating AS media_rating, media.backdropImage AS media_backdropImage, media.releaseDate AS media_releaseDate, media.createdAt AS media_createdAt, media.updatedAt AS media_updatedAt FROM library_list_items INNER JOIN media ON library_list_items.mediaId = media.id"
        )
    }

    private fun migrateMovieWatchHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `movies_watch_history_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `mediaId` TEXT NOT NULL,
                    `ownerId` TEXT NOT NULL,
                    `progress` INTEGER NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`mediaId`) REFERENCES `media`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                    FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent()
        )

        db.execSQL(
            """
                INSERT INTO `movies_watch_history_new` (
                    id, mediaId, ownerId, progress, duration, status, createdAt, updatedAt
                )
                SELECT
                    id, filmId, ownerId, progress, duration, status, createdAt, updatedAt
                FROM `movies_watch_history`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `movies_watch_history`")
        db.execSQL("ALTER TABLE `movies_watch_history_new` RENAME TO `movies_watch_history`")

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_movies_watch_history_mediaId`
            ON `movies_watch_history` (`mediaId`)
        """
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_movies_watch_history_ownerId`
            ON `movies_watch_history` (`ownerId`)
        """
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_movies_watch_history_mediaId_ownerId`
            ON `movies_watch_history` (`mediaId`, `ownerId`)
        """
        )
    }

    private fun migrateSeriesWatchHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `series_watch_history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `mediaId` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `progress` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `duration` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `seasonNumber` INTEGER NOT NULL,
                `episodeNumber` INTEGER NOT NULL,
                FOREIGN KEY(`mediaId`) REFERENCES `media`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `series_watch_history_new` (
                `id`, `mediaId`, `ownerId`, `progress`, `status`, `duration`,
                `createdAt`, `updatedAt`, `seasonNumber`, `episodeNumber`
            )
            SELECT `id`, `filmId`, `ownerId`, `progress`, `status`, `duration`,
                `createdAt`, `updatedAt`, `seasonNumber`, `episodeNumber`
            FROM `series_watch_history`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `series_watch_history`")
        db.execSQL("ALTER TABLE `series_watch_history_new` RENAME TO `series_watch_history`")

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_series_watch_history_mediaId_ownerId_seasonNumber_episodeNumber` ON `series_watch_history` (`mediaId`, `ownerId`, `seasonNumber`, `episodeNumber`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_series_watch_history_mediaId` ON `series_watch_history` (`mediaId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_series_watch_history_ownerId` ON `series_watch_history` (`ownerId`)",
        )
    }
}
