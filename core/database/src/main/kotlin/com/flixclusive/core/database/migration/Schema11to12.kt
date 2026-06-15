package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Locale

internal object Schema11to12 : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateDbFilms(db)
        migrateInstalledProviders(db)
        recreateListItemMetadataView(db)
    }

    private fun migrateReleaseDates(db: SupportSQLiteDatabase) {
        val locale = Locale.US

        val cursor = db.query("SELECT `id`, `releaseDate` FROM `films` WHERE `releaseDate` IS NOT NULL")
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getString(0)
                val dateString = it.getString(1) ?: continue

                val epochMillis = runCatching {
                    val format = when {
                        dateString.contains(",") -> "MMMM d, yyyy"
                        dateString.contains("-") -> "yyyy-MM-dd"
                        else -> return@runCatching null
                    }
                    SimpleDateFormat(format, locale).parse(dateString)?.time
                }.getOrNull() ?: continue // skip unparseable / blank rows

                db.execSQL(
                    "UPDATE `films` SET `releaseDate` = ? WHERE `id` = ?",
                    arrayOf(epochMillis, id)
                )
            }
        }
    }

    private fun migrateDbFilms(db: SupportSQLiteDatabase) {
        migrateReleaseDates(db)

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `films_new` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `providerId` TEXT NOT NULL,
                `filmType` TEXT NOT NULL,
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
            INSERT INTO `films_new` (
                `id`, `providerId`, `filmType`, `title`, `posterImage`, `language`,
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
        db.execSQL("ALTER TABLE `films_new` RENAME TO `films`")
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
                `id`            TEXT    NOT NULL,
                `ownerId`       TEXT    NOT NULL,
                `repositoryUrl` TEXT    NOT NULL,
                `filePath`      TEXT    NOT NULL,
                `sortOrder`     REAL    NOT NULL,
                `isEnabled`     INTEGER NOT NULL,
                `isDebug`       INTEGER NOT NULL,
                `createdAt`     INTEGER NOT NULL,
                `updatedAt`     INTEGER NOT NULL,
                PRIMARY KEY (`id`, `ownerId`),
                FOREIGN KEY (`repositoryUrl`, `ownerId`)
                    REFERENCES `repositories` (`url`, `userId`)
                    ON DELETE CASCADE
                    ON UPDATE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_repositoryUrl` ON `installed_providers` (`repositoryUrl`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_ownerId` ON `installed_providers` (`ownerId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_repositoryUrl_ownerId` ON `installed_providers` (`repositoryUrl`, `ownerId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_sortOrder` ON `installed_providers` (`sortOrder`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_isEnabled` ON `installed_providers` (`isEnabled`)"
        )

        db.execSQL(
            """
            INSERT INTO `installed_providers`
                (`id`, `ownerId`, `repositoryUrl`, `filePath`, `sortOrder`, `isEnabled`, `isDebug`, `createdAt`, `updatedAt`)
            SELECT
                `id`, `ownerId`, `repositoryUrl`, `filePath`, `sortOrder`, `isEnabled`, `isDebug`, `createdAt`, `updatedAt`
            FROM `installed_providers_old`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `installed_providers_old`")
    }

    private fun recreateListItemMetadataView(db: SupportSQLiteDatabase) {
        db.execSQL("DROP VIEW IF EXISTS `library_list_item_with_metadata`")
        db.execSQL(
            "CREATE VIEW `library_list_item_with_metadata` AS SELECT library_list_items.id AS item_id, library_list_items.filmId AS item_filmId, library_list_items.listId AS item_listId, library_list_items.createdAt AS item_createdAt, library_list_items.updatedAt AS item_updatedAt, films.id AS film_id, films.title AS film_title, films.providerId AS film_providerId, films.filmType AS film_filmType, films.overview AS film_overview, films.posterImage AS film_posterImage, films.adult AS film_adult, films.language AS film_language, films.rating AS film_rating, films.backdropImage AS film_backdropImage, films.releaseDate AS film_releaseDate, films.createdAt AS film_createdAt, films.updatedAt AS film_updatedAt FROM library_list_items INNER JOIN films ON library_list_items.filmId = films.id"
        )
    }
}
