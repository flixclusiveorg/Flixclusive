package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Gives `download_items` a unique identity for "the same download".
 *
 * Two rows for one episode both resolve to the same filename and write to the same file, and until
 * now nothing stopped that — the primary key is a random UUID and the only guard was a
 * check-then-insert in the UI that two quick taps can both pass.
 *
 * The constraint is a non-null `dedupeKey` rather than a unique index over
 * `(mediaId, seasonNumber, episodeNumber)`, because SQLite treats NULLs as distinct: movies carry no
 * season or episode, so an index over those columns would let every movie through. The key's format
 * must match [com.flixclusive.core.database.entity.downloads.dedupeKeyOf] exactly.
 *
 * Duplicates already in the database have to go before the index can exist. The survivor is the row
 * that got furthest, so the migration keeps whatever progress is actually on disk.
 */
internal object Schema22to23 : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `download_items` ADD COLUMN `dedupeKey` TEXT NOT NULL DEFAULT ''")

        // COALESCE mirrors dedupeKeyOf's `?: -1` — the two must agree character for character or a
        // runtime insert could slip past the index that this backfill built.
        db.execSQL(
            """
            UPDATE `download_items`
            SET `dedupeKey` = `mediaId` || '|' || COALESCE(`seasonNumber`, -1) || '|' || COALESCE(`episodeNumber`, -1)
            """.trimIndent(),
        )

        // Chunks are deleted explicitly: Room runs migrations with foreign keys disabled, so the
        // ON DELETE CASCADE from download_chunks would not fire and the rows would be orphaned.
        db.execSQL(
            """
            DELETE FROM `download_chunks`
            WHERE `downloadItemId` NOT IN ($SURVIVORS_QUERY)
            """.trimIndent(),
        )

        db.execSQL(
            """
            DELETE FROM `download_items`
            WHERE `id` NOT IN ($SURVIVORS_QUERY)
            """.trimIndent(),
        )

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_download_items_dedupeKey` ON `download_items` (`dedupeKey`)",
        )
    }

    /**
     * Exactly one row per `dedupeKey`: the one that downloaded the most, ties broken by the newest
     * queue time. Keeping the furthest-along row means the bytes already on disk stay accounted for.
     *
     * Written as a correlated pick rather than `GROUP BY … HAVING x = MAX(x)`: that form relies on
     * SQLite's bare-column quirk, and with two aggregates it can match no row at all for a group —
     * which here would have deleted the entire group instead of its duplicates.
     */
    private const val SURVIVORS_QUERY =
        """
        SELECT d.`id` FROM `download_items` AS d
        WHERE d.`rowid` = (
            SELECT x.`rowid` FROM `download_items` AS x
            WHERE x.`dedupeKey` = d.`dedupeKey`
            ORDER BY x.`streamBytesDownloaded` DESC, x.`createdAt` DESC
            LIMIT 1
        )
        """
}
