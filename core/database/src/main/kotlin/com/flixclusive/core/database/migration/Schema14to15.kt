package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

internal object Schema14to15 : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateLibraryListItemsPrimaryKey(db)
    }

    private fun migrateLibraryListItemsPrimaryKey(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_list_items_new` (
                `id` TEXT NOT NULL,
                `mediaId` TEXT NOT NULL,
                `listId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`listId`) REFERENCES `library_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`mediaId`) REFERENCES `media`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )

        val cursor = db.query(
            "SELECT `mediaId`, `listId`, `createdAt`, `updatedAt` FROM `library_list_items`",
        )

        cursor.use { cursor ->
            val mediaIdIndex = cursor.getColumnIndexOrThrow("mediaId")
            val listIdIndex = cursor.getColumnIndexOrThrow("listId")
            val createdAtIndex = cursor.getColumnIndexOrThrow("createdAt")
            val updatedAtIndex = cursor.getColumnIndexOrThrow("updatedAt")

            while (cursor.moveToNext()) {
                val newId = UUID.randomUUID().toString()
                val mediaId = cursor.getString(mediaIdIndex)
                val listId = cursor.getString(listIdIndex)
                val createdAt = cursor.getLong(createdAtIndex)
                val updatedAt = cursor.getLong(updatedAtIndex)

                db.execSQL(
                    """
                        INSERT INTO `library_list_items_new` (
                            `id`, `mediaId`, `listId`, `createdAt`, `updatedAt`
                        ) VALUES (?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(newId, mediaId, listId, createdAt, updatedAt),
                )
            }
        }

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
}
