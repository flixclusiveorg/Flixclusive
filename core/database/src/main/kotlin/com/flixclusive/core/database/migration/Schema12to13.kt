package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

internal object Schema12to13 : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP VIEW IF EXISTS `library_list_item_with_metadata`")

        db.execSQL("DROP INDEX IF EXISTS `index_library_list_items_filmId_listId`")
        db.execSQL("DROP INDEX IF EXISTS `index_library_list_items_filmId`")
        db.execSQL("DROP INDEX IF EXISTS `index_library_list_items_listId`")
        db.execSQL("ALTER TABLE `library_list_items` RENAME TO `library_list_items_old`")
        db.execSQL("DROP INDEX IF EXISTS `index_library_lists_ownerId`")
        db.execSQL("DROP INDEX IF EXISTS `index_library_lists_ownerId_name`")
        db.execSQL("ALTER TABLE `library_lists` RENAME TO `library_lists_old`")

        val listIdMapping = migrateLibraryLists(db)
        migrateLibraryListItems(db, listIdMapping)

        db.execSQL("DROP TABLE `library_list_items_old`")
        db.execSQL("DROP TABLE `library_lists_old`")

        recreateListItemMetadataView(db)
    }

    private fun migrateLibraryLists(db: SupportSQLiteDatabase): Map<Int, String> {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_lists` (
                `id` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `listType` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_lists_ownerId` ON `library_lists` (`ownerId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_library_lists_ownerId_name` ON `library_lists` (`ownerId`, `name`)"
        )

        val mapping = mutableMapOf<Int, String>()

        val cursor = db.query(
            """
            SELECT `id`, `ownerId`, `name`, `description`, `listType`, `createdAt`, `updatedAt`
            FROM `library_lists_old`
            """.trimIndent()
        )

        cursor.use {
            while (it.moveToNext()) {
                val oldId = it.getInt(0)
                val newId = UUID.randomUUID().toString()

                mapping[oldId] = newId

                val ownerId = it.getString(1)
                val name = it.getString(2)
                val description = it.getString(3)
                val listType = it.getString(4)
                val createdAt = it.getLong(5)
                val updatedAt = it.getLong(6)

                db.execSQL(
                    """
                    INSERT INTO `library_lists` (
                        `id`, `ownerId`, `name`, `description`, `listType`, `createdAt`, `updatedAt`
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(newId, ownerId, name, description, listType, createdAt, updatedAt)
                )
            }
        }

        return mapping
    }

    private fun migrateLibraryListItems(
        db: SupportSQLiteDatabase,
        listIdMapping: Map<Int, String>,
    ) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_list_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `listId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`listId`) REFERENCES `library_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_library_list_items_filmId_listId` ON `library_list_items` (`filmId`, `listId`)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_list_items_filmId` ON `library_list_items` (`filmId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_list_items_listId` ON `library_list_items` (`listId`)")

        val cursor = db.query(
            """
            SELECT `id`, `filmId`, `listId`, `createdAt`, `updatedAt`
            FROM `library_list_items_old`
            """.trimIndent()
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val filmId = it.getString(1)
                val oldListId = it.getInt(2)
                val createdAt = it.getLong(3)
                val updatedAt = it.getLong(4)

                val newListId = listIdMapping[oldListId] ?: continue

                db.execSQL(
                    """
                    INSERT INTO `library_list_items` (`id`, `filmId`, `listId`, `createdAt`, `updatedAt`)
                    VALUES (?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(id, filmId, newListId, createdAt, updatedAt)
                )
            }
        }
    }

    private fun recreateListItemMetadataView(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE VIEW `library_list_item_with_metadata` AS SELECT library_list_items.id AS item_id, library_list_items.filmId AS item_filmId, library_list_items.listId AS item_listId, library_list_items.createdAt AS item_createdAt, library_list_items.updatedAt AS item_updatedAt, films.id AS film_id, films.title AS film_title, films.providerId AS film_providerId, films.filmType AS film_filmType, films.overview AS film_overview, films.posterImage AS film_posterImage, films.adult AS film_adult, films.language AS film_language, films.rating AS film_rating, films.backdropImage AS film_backdropImage, films.releaseDate AS film_releaseDate, films.createdAt AS film_createdAt, films.updatedAt AS film_updatedAt FROM library_list_items INNER JOIN films ON library_list_items.filmId = films.id"
        )
    }
}
