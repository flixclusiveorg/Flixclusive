package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Creates the download tables.
 *
 * `dedupeKey` gives `download_items` a unique identity for "the same download". Two rows for one
 * episode both resolve to the same filename and write to the same file, and nothing else stops
 * that — the primary key is a random UUID and the only other guard is a check-then-insert in the UI
 * that two quick taps can both pass.
 *
 * The constraint is a non-null `dedupeKey` column rather than a unique index over
 * `(mediaId, seasonNumber, episodeNumber)`, because SQLite treats NULLs as distinct: movies carry no
 * season or episode, so an index over those columns would let every movie through. The key's format
 * must match [com.flixclusive.core.database.entity.downloads.dedupeKeyOf] exactly.
 */
internal object Schema20to21 : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `download_items` (
                `id` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `mediaId` TEXT NOT NULL,
                `mediaTitle` TEXT NOT NULL,
                `mediaType` TEXT NOT NULL,
                `seasonNumber` INTEGER,
                `episodeNumber` INTEGER,
                `dedupeKey` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `phase` TEXT,
                `sourceUrl` TEXT,
                `isHlsStream` INTEGER NOT NULL,
                `streamFilePath` TEXT,
                `streamBytesDownloaded` INTEGER NOT NULL,
                `streamTotalBytes` INTEGER NOT NULL,
                `downloadBytesPerSecond` INTEGER NOT NULL,
                `downloadedSubtitlesCount` INTEGER NOT NULL,
                `totalSubtitlesCount` INTEGER NOT NULL,
                `errorMessage` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_items_mediaId` ON `download_items` (`mediaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_items_state` ON `download_items` (`state`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_items_ownerId` ON `download_items` (`ownerId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_download_items_dedupeKey` ON `download_items` (`dedupeKey`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `download_chunks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `downloadItemId` TEXT NOT NULL,
                `chunkIndex` INTEGER NOT NULL,
                `rangeStart` INTEGER NOT NULL,
                `rangeEnd` INTEGER NOT NULL,
                `bytesDownloaded` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                FOREIGN KEY(`downloadItemId`) REFERENCES `download_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_download_chunks_downloadItemId` ON `download_chunks` (`downloadItemId`)",
        )
    }
}
