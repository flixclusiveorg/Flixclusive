package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Schema20to21 : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `download_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `mediaId` TEXT NOT NULL,
                `mediaTitle` TEXT NOT NULL,
                `mediaType` TEXT NOT NULL,
                `seasonNumber` INTEGER,
                `episodeNumber` INTEGER,
                `episodeTitle` TEXT,
                `state` TEXT NOT NULL,
                `phase` TEXT,
                `streamUrl` TEXT,
                `streamBytesDownloaded` INTEGER NOT NULL,
                `streamTotalBytes` INTEGER NOT NULL,
                `subtitleBytesDownloaded` INTEGER NOT NULL,
                `subtitleTotalBytes` INTEGER NOT NULL,
                `subtitleError` TEXT,
                `errorMessage` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_items_mediaId` ON `download_items` (`mediaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_items_state` ON `download_items` (`state`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `download_chunks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `downloadItemId` INTEGER NOT NULL,
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
