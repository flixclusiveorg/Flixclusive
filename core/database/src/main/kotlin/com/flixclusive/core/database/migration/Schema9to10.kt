package com.flixclusive.core.database.migration

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flixclusive.core.database.R

/**
 * Major migration from schema version 9 to 10.
 *
 * This migration consolidates and restructures the database to support a unified
 * library system, provider installation tracking, and a more extensible film
 * external ID model. It is non-destructive where possible — existing user data,
 * films, search history, and watchlist entries are all preserved and migrated
 * into their new structures.
 *
 * ## Changes
 *
 * ### [User]
 * - Added `createdAt` and `updatedAt` timestamp columns (backfilled with migration time).
 *
 * ### [DBFilm] (`films`)
 * - Removed `imdbId` and `tmdbId` hardcoded columns.
 * - Added `year` column.
 * - Added `createdAt` and `updatedAt` timestamp columns.
 * - External IDs (`imdb`, `tmdb`) are migrated into the new [FilmExternalId]
 *   (`film_external_ids`) table, keyed by `(filmId, providerId, source)`.
 *   Only non-null values are migrated.
 *
 * ### [FilmExternalId] (`film_external_ids`) — NEW
 * - Introduced to replace hardcoded `imdbId`/`tmdbId` columns with an extensible
 *   key-value model supporting any metadata source (e.g. `"imdb"`, `"tmdb"`, `"tvdb"`).
 * - Unique constraint on `(filmId, providerId, source)` to allow different providers
 *   to hold differing external IDs for the same film without conflict.
 *
 * ### [SearchHistory] (`search_history`)
 * - Added `updatedAt` column.
 * - Renamed `searchedOn` → `createdAt` for timestamp uniformity; `updatedAt`
 *   is backfilled with the same `searchedOn` value.
 *
 * ### [LibraryList] (`library_lists`)
 * - Added `listType` column (`WATCHLIST`, `CONTINUE_WATCHING`, `CUSTOM`),
 *   defaulting to `CUSTOM` for all pre-existing rows.
 * - System lists (`WATCHLIST`, `RECENTLY_WATCHED`) are seeded per user.
 *
 * ### `watchlist` — REMOVED
 * - The standalone `watchlist` table is dropped. All existing watchlist entries
 *   per user are migrated into `library_list_items` under the seeded `CUSTOM`
 *   system list. Original `addedAt` timestamps are preserved as `createdAt`
 *   and `updatedAt`.
 *
 * ### [Repository] (`repositories`) — NEW
 * - Stores provider repository metadata: `url` (PK), `owner`, `name`,
 *   `rawLinkFormat`.
 *
 * ### [InstalledProvider] (`installed_providers`) — NEW
 * - Replaces the old `ProviderFromPreferences` DataStore model with a proper
 *   Room entity. Stores operational provider fields from [ProviderMetadata]
 *   alongside app-managed fields (`isDisabled`, `isDebug`, `sortOrder`).
 * - `sortOrder` is a `REAL` (float) column supporting fractional/midpoint
 *   ordering for drag-and-drop reordering without full-table index shifts.
 * - FK on `repositoryUrl` references `repositories.url` with `ON UPDATE CASCADE
 *   ON DELETE CASCADE`, ensuring providers are cleaned up when their repository
 *   is removed, and URL changes propagate automatically.
 *
 * ### `library_list_item_with_metadata` (VIEW)
 * - Recreated to reflect the updated `films` schema after column changes.
 */
internal class Schema9to10(private val context: Context) : Migration(startVersion = 9, endVersion = 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        migrateUserTable(db, now)
        migrateFilmsTable(db, now)
        migrateSearchHistory(db)
        migrateLibraryLists(db)
        migrateLibraryListItems(db)
        seedAndMigrateSystemLists(db, now)
        migrateMoviesWatchHistory(db)
        migrateSeriesWatchHistory(db)
        createRepositoriesTable(db)
        createInstalledProvidersTable(db)
        recreateView(db)
        createDbFilmsFstTable(db)
    }

    private fun migrateUserTable(db: SupportSQLiteDatabase, now: Long) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `User_new` (
                `userId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `image` INTEGER NOT NULL,
                `pin` TEXT DEFAULT NULL,
                `pinHint` TEXT DEFAULT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `User_new` (`userId`, `name`, `image`, `pin`, `pinHint`, `createdAt`, `updatedAt`)
            SELECT `userId`, `name`, `image`, `pin`, `pinHint`, $now, $now FROM `User`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `User`")
        db.execSQL("ALTER TABLE `User_new` RENAME TO `User`")
    }

    private fun migrateFilmsTable(db: SupportSQLiteDatabase, now: Long) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `film_external_ids` (
                filmId TEXT NOT NULL,
                providerId TEXT NOT NULL,
                source TEXT NOT NULL,
                externalId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY (filmId, providerId, source),
                FOREIGN KEY (filmId) REFERENCES films(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_film_external_ids_filmId`
            ON `film_external_ids` (`filmId`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_film_external_ids_providerId`
            ON `film_external_ids` (`providerId`)
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO `film_external_ids`
                (`filmId`, `providerId`, `source`, `externalId`, `createdAt`, `updatedAt`)
            SELECT `id`, `providerId`, 'imdb', `imdbId`, $now, $now
            FROM `films`
            WHERE `imdbId` IS NOT NULL
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO `film_external_ids`
                (`filmId`, `providerId`, `source`, `externalId`, `createdAt`, `updatedAt`)
            SELECT `id`, `providerId`, 'tmdb', CAST(`tmdbId` AS TEXT), $now, $now
            FROM `films`
            WHERE `tmdbId` IS NOT NULL
            """.trimIndent()
        )

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
                `releaseDate` TEXT,
                `year` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `films_new` (
                `id`, `providerId`, `filmType`, `title`, `posterImage`, `language`,
                `adult`, `overview`, `rating`, `backdropImage`, `releaseDate`, `year`,
                `createdAt`, `updatedAt`
            )
            SELECT `id`, `providerId`, `filmType`, `title`, `posterImage`, `language`,
                `adult`, `overview`, `rating`, `backdropImage`, `releaseDate`,
                COALESCE(strftime('%Y', `releaseDate`), 0), `createdAt`, `updatedAt`
            FROM `films`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `films`")
        db.execSQL("ALTER TABLE `films_new` RENAME TO `films`")
    }

    private fun migrateSearchHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `search_history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `query` TEXT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `search_history_new` (`id`, `query`, `ownerId`, `createdAt`, `updatedAt`)
            SELECT `id`, `query`, `ownerId`, `searchedOn`, `searchedOn` FROM `search_history`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `search_history`")
        db.execSQL("ALTER TABLE `search_history_new` RENAME TO `search_history`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query_ownerId` ON `search_history` (`query`, `ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_ownerId` ON `search_history` (`ownerId`)")
    }

    private fun migrateLibraryLists(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_lists_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `listType` TEXT NOT NULL DEFAULT 'CUSTOM',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `library_lists_new` (`id`, `ownerId`, `name`, `description`, `listType`, `createdAt`, `updatedAt`)
            SELECT `listId`, `ownerId`, `name`, `description`, 'CUSTOM', `createdAt`, `updatedAt` FROM `library_lists`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `library_lists`")
        db.execSQL("ALTER TABLE `library_lists_new` RENAME TO `library_lists`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_lists_ownerId` ON `library_lists` (`ownerId`)")
    }

    private fun migrateLibraryListItems(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_list_items_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `listId` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`listId`) REFERENCES `library_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `library_list_items_new` (`id`, `filmId`, `listId`, `createdAt`, `updatedAt`)
            SELECT `itemId`, `filmId`, `listId`, `addedAt`, `addedAt` FROM `library_list_items`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `library_list_items`")
        db.execSQL("ALTER TABLE `library_list_items_new` RENAME TO `library_list_items`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_library_list_items_filmId_listId` ON `library_list_items` (`filmId`, `listId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_list_items_filmId` ON `library_list_items` (`filmId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_list_items_listId` ON `library_list_items` (`listId`)")
    }

    private fun migrateMoviesWatchHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `movies_watch_history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `progress` INTEGER NOT NULL,
                `duration` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `movies_watch_history_new` (
                `id`, `filmId`, `ownerId`, `progress`, `duration`,
                `status`, `createdAt`, `updatedAt`
            )
            SELECT
                `id`, `filmId`, `ownerId`, `progress`,
                COALESCE(`duration`, 0),
                `status`,
                `watchedAt`, `watchedAt`
            FROM `movies_watch_history`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `movies_watch_history`")
        db.execSQL("ALTER TABLE `movies_watch_history_new` RENAME TO `movies_watch_history`")

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_movies_watch_history_filmId_ownerId`
            ON `movies_watch_history` (`filmId`, `ownerId`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_movies_watch_history_filmId`
            ON `movies_watch_history` (`filmId`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_movies_watch_history_ownerId`
            ON `movies_watch_history` (`ownerId`)
            """.trimIndent()
        )
    }

    private fun migrateSeriesWatchHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `series_watch_history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `progress` INTEGER NOT NULL,
                `duration` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `seasonNumber` INTEGER NOT NULL,
                `episodeNumber` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `series_watch_history_new` (
                `id`, `filmId`, `ownerId`, `progress`, `duration`,
                `status`, `seasonNumber`, `episodeNumber`, `createdAt`, `updatedAt`
            )
            SELECT
                `id`, `filmId`, `ownerId`, `progress`,
                COALESCE(`duration`, 0),
                `status`, `seasonNumber`, `episodeNumber`,
                `watchedAt`, `watchedAt`
            FROM `series_watch_history`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `series_watch_history`")
        db.execSQL("ALTER TABLE `series_watch_history_new` RENAME TO `series_watch_history`")

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_series_watch_history_filmId_ownerId_seasonNumber_episodeNumber`
            ON `series_watch_history` (`filmId`, `ownerId`, `seasonNumber`, `episodeNumber`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_series_watch_history_filmId`
            ON `series_watch_history` (`filmId`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_series_watch_history_ownerId`
            ON `series_watch_history` (`ownerId`)
            """.trimIndent()
        )
    }

    private fun seedAndMigrateSystemLists(db: SupportSQLiteDatabase, now: Long) {
        val userCursor = db.query("SELECT userId FROM User")
        val userIds = mutableListOf<Int>()
        while (userCursor.moveToNext()) {
            userIds.add(userCursor.getInt(0))
        }
        userCursor.close()

        val watchlist = context.getString(R.string.seeded_watchlist)
        val recentlyWatched = context.getString(R.string.seeded_recently_watched)

        val watchlistDescription = context.getString(R.string.seeded_watchlist_description)
        val recentlyWatchedDescription = context.getString(R.string.seeded_recently_watched_description)

        for (userId in userIds) {
            db.execSQL(
                """
                INSERT INTO `library_lists` (`ownerId`, `name`, `description`, `listType`, `createdAt`, `updatedAt`)
                VALUES (?, '$recentlyWatched', '$recentlyWatchedDescription', 'WATCHED', ?, ?)
                """.trimIndent(),
                arrayOf<Any>(userId, now, now)
            )

            db.execSQL(
                """
                INSERT INTO `library_lists` (`ownerId`, `name`, `description`, `listType`, `createdAt`, `updatedAt`)
                VALUES (?, '$watchlist', '$watchlistDescription', 'CUSTOM', ?, ?)
                """.trimIndent(),
                arrayOf<Any>(userId, now, now)
            )
            val watchlistIdCursor = db.query("SELECT last_insert_rowid()")
            watchlistIdCursor.moveToLast()
            val watchlistId = watchlistIdCursor.getInt(0)
            watchlistIdCursor.close()

            // Migrate watchlist items for this user into the new watchlist library list
            val watchlistCursor = db.query(
                "SELECT filmId, addedAt FROM watchlist WHERE ownerId = ?",
                arrayOf<Any>(userId.toString())
            )
            while (watchlistCursor.moveToNext()) {
                val filmId = watchlistCursor.getString(0)
                val addedAt = watchlistCursor.getLong(1)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `library_list_items` (`filmId`, `listId`, `createdAt`, `updatedAt`)
                    VALUES (?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(filmId, watchlistId, addedAt, addedAt)
                )
            }

            // Migrate movies watch history entries for this user into the recently watched library list
            val recentlyWatchedListIdCursor = db.query(
                "SELECT id FROM library_lists WHERE ownerId = ? AND listType = 'WATCHED'",
                arrayOf<Any>(userId.toString())
            )
            recentlyWatchedListIdCursor.moveToFirst()
            val recentlyWatchedListId = recentlyWatchedListIdCursor.getInt(0)
            recentlyWatchedListIdCursor.close()

            val moviesWatchHistoryCursor = db.query(
                "SELECT filmId, watchedAt FROM movies_watch_history WHERE ownerId = ?",
                arrayOf<Any>(userId.toString())
            )
            while (moviesWatchHistoryCursor.moveToNext()) {
                val filmId = moviesWatchHistoryCursor.getString(0)
                val watchedAt = moviesWatchHistoryCursor.getLong(1)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `library_list_items` (`filmId`, `listId`, `createdAt`, `updatedAt`)
                    VALUES (?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(filmId, recentlyWatchedListId, watchedAt, watchedAt)
                )
            }
            watchlistCursor.close()

            // Migrate series watch history entries for this user into the recently watched library list
            val seriesWatchHistoryCursor = db.query(
                "SELECT filmId, watchedAt FROM series_watch_history WHERE ownerId = ?",
                arrayOf<Any>(userId.toString())
            )

            while (seriesWatchHistoryCursor.moveToNext()) {
                val filmId = seriesWatchHistoryCursor.getString(0)
                val watchedAt = seriesWatchHistoryCursor.getLong(1)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `library_list_items` (`filmId`, `listId`, `createdAt`, `updatedAt`)
                    VALUES (?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(filmId, recentlyWatchedListId, watchedAt, watchedAt)
                )
            }
        }

        db.execSQL("DROP TABLE IF EXISTS `watchlist`")
    }

    private fun createRepositoriesTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `repositories` (
                `url` TEXT NOT NULL PRIMARY KEY,
                `userId` INTEGER NOT NULL,
                `owner` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `rawLinkFormat` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`userId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_repositories_userId` ON `repositories` (`userId`)")
    }

    private fun createInstalledProvidersTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `installed_providers` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `ownerId` INTEGER NOT NULL,
                `repositoryUrl` TEXT NOT NULL,
                `filePath` TEXT NOT NULL,
                `sortOrder` REAL NOT NULL,
                `isEnabled` INTEGER NOT NULL,
                `isDebug` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`repositoryUrl`) REFERENCES `repositories`(`url`) ON UPDATE CASCADE ON DELETE CASCADE
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_providers_repositoryUrl` ON `installed_providers` (`repositoryUrl`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_providers_sortOrder` ON `installed_providers` (`sortOrder`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_providers_ownerId` ON `installed_providers` (`ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_providers_isEnabled` ON `installed_providers` (`isEnabled`)")
    }

    private fun recreateView(db: SupportSQLiteDatabase) {
        db.execSQL("DROP VIEW IF EXISTS `library_list_item_with_metadata`")
        db.execSQL("CREATE VIEW `library_list_item_with_metadata` AS SELECT library_list_items.id AS item_id, library_list_items.filmId AS item_filmId, library_list_items.listId AS item_listId, library_list_items.createdAt AS item_createdAt, library_list_items.updatedAt AS item_updatedAt, films.id AS film_id, films.title AS film_title, films.providerId AS film_providerId, films.filmType AS film_filmType, films.overview AS film_overview, films.posterImage AS film_posterImage, films.adult AS film_adult, films.language AS film_language, films.rating AS film_rating, films.backdropImage AS film_backdropImage, films.releaseDate AS film_releaseDate, films.year AS film_year, films.createdAt AS film_createdAt, films.updatedAt AS film_updatedAt FROM library_list_items INNER JOIN films ON library_list_items.filmId = films.id")
    }

    private fun createDbFilmsFstTable(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS `films_fts`
            USING fts3(
                `filmId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `overview` TEXT NOT NULL
            )
        """)

        // Optional backfill for existing data
        db.execSQL("""
            INSERT INTO films_fts (filmId, title, overview)
            SELECT id, title, COALESCE(overview, '') FROM films
        """)
    }
}
