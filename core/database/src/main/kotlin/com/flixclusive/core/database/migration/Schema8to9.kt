package com.flixclusive.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.film.DBFilm.Companion.DB_FILM_VALID_RECOMMENDATIONS_COUNT
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.database.migration.Schema8to9.DBFilmMigrator.toDBFilm
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.json.fromJson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Major migration from schema version 8 to 9.
 *
 * Changes:
 * - Introduces a new `films` table to store film data.
 * - Migrates film data from `library_list_items`, `watch_history`, and `watchlist` tables into the new `films` table.
 * - Restructures `library_list_items` table to reference films by ID instead of storing film data directly.
 * - Restructures `watchlist` table to reference films by ID instead of storing film data directly.
 * - Splits `watch_history` table into `movies_watch_history` and `series_watch_history` tables.
 * - Updates `library_list_and_item_cross_ref` to use Long itemId instead of String.
 * - Updates watchlist table structure with auto-generated primary key and foreign key reference to films.
 * - Ensures that each film is only inserted once, even if it appears in multiple tables.
 * - Adds `createdAt` and `updatedAt` timestamps to the `films` table, initialized to the current time.
 * - Converts `customProperties` from a Map to a JSON string for storage in the database.
 * - Handles migration of existing watch history data, converting it to the new structure.
 * - Adds new indices for improved query performance.
 */
internal object Schema8to9 : Migration(8, 9) {
    private val processedFilmIds = mutableSetOf<String>()

    override fun migrate(db: SupportSQLiteDatabase) {
        // Create the films table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `films` (
                `id` TEXT NOT NULL,
                `providerId` TEXT NOT NULL DEFAULT '',
                `imdbId` TEXT,
                `tmdbId` INTEGER,
                `language` TEXT,
                `adult` INTEGER NOT NULL DEFAULT 0,
                `title` TEXT NOT NULL DEFAULT '',
                `runtime` INTEGER,
                `backdropImage` TEXT,
                `posterImage` TEXT,
                `overview` TEXT,
                `homePage` TEXT,
                `releaseDate` TEXT,
                `logoImage` TEXT,
                `year` INTEGER,
                `filmType` TEXT NOT NULL DEFAULT 'MOVIE',
                `rating` REAL,
                `customProperties` TEXT NOT NULL DEFAULT '{}',
                `hasRecommendations` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )

        // Create new watch history tables
        createWatchHistoryTables(db)

        // Process films from all tables first
        processFilmsFromTable(db, "library_list_items")
        processFilmsFromTable(db, "watch_history")
        processFilmsFromTable(db, "watchlist")

        // Migrate table structures
        migrateSearchHistory(db)
        migrateLibraryList(db)
        migrateLibraryListItems(db)
        migrateWatchlist(db)
        migrateWatchHistory(db)

        db.execSQL(
            "CREATE VIEW `library_list_item_with_metadata` AS SELECT library_list_items.*, films.* FROM library_list_items INNER JOIN films ON library_list_items.filmId = films.id",
        )
    }

    private fun createWatchHistoryTables(db: SupportSQLiteDatabase) {
        // Create movies_watch_history table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `movies_watch_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `progress` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `duration` INTEGER NOT NULL DEFAULT 0,
                `watchedAt` INTEGER NOT NULL,
                `watchCount` INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        // Create series_watch_history table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `series_watch_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `progress` INTEGER NOT NULL,
                `duration` INTEGER NOT NULL DEFAULT 0,
                `status` TEXT NOT NULL,
                `watchedAt` INTEGER NOT NULL,
                `seasonNumber` INTEGER NOT NULL,
                `episodeNumber` INTEGER NOT NULL,
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        // Create indices
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_movies_watch_history_filmId_ownerId` ON `movies_watch_history` (`filmId`, `ownerId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_movies_watch_history_filmId` ON `movies_watch_history` (`filmId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_movies_watch_history_ownerId` ON `movies_watch_history` (`ownerId`)",
        )

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_series_watch_history_filmId_ownerId_seasonNumber_episodeNumber` ON `series_watch_history` (`filmId`, `ownerId`, `seasonNumber`, `episodeNumber`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_series_watch_history_filmId` ON `series_watch_history` (`filmId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_series_watch_history_ownerId` ON `series_watch_history` (`ownerId`)",
        )
    }

    private fun migrateWatchHistory(db: SupportSQLiteDatabase) {
        // Migrate data from old watch_history table
        val cursor =
            db.query("SELECT id, film, ownerId, seasons, episodes, episodesWatched, dateWatched FROM watch_history")
        while (cursor.moveToNext()) {
            val oldId = cursor.getString(0)
            val filmJson = cursor.getString(1)
            val ownerId = cursor.getInt(2)
            val episodesWatchedJson = cursor.getString(5) // List<EpisodeWatched> as JSON
            val dateWatched = cursor.getLong(6)

            try {
                val dbFilm = filmJson.toDBFilm()
                if (dbFilm.id.isNotEmpty()) {
                    // Check if this is a movie or series based on filmType
                    if (dbFilm.filmType.name == "MOVIE") {
                        // Migrate as MovieProgress
                        migrateMovieProgress(
                            db = db,
                            filmId = dbFilm.id,
                            ownerId = ownerId,
                            episodesWatchedJson = episodesWatchedJson,
                            dateWatched = dateWatched,
                        )
                    } else {
                        // Migrate as EpisodeProgress for series
                        migrateEpisodeProgress(
                            db = db,
                            filmId = dbFilm.id,
                            ownerId = ownerId,
                            episodesWatchedJson = episodesWatchedJson,
                            dateWatched = dateWatched,
                        )
                    }
                }
            } catch (e: Exception) {
                errorLog("Error migrating watch history item $oldId: ${e.message}")
            }
        }
        cursor.close()

        // Drop the old watch_history table
        db.execSQL("DROP TABLE watch_history")
    }

    private fun migrateMovieProgress(
        db: SupportSQLiteDatabase,
        filmId: String,
        ownerId: Int,
        episodesWatchedJson: String,
        dateWatched: Long,
    ) {
        try {
            val episodesWatched = parseEpisodesWatched(episodesWatchedJson)

            // For movies, take the first episode watched or create a default entry
            val movieProgress = episodesWatched.firstOrNull()
            val progress = movieProgress?.watchTime ?: 0L
            val duration = movieProgress?.durationTime ?: 0L
            val status = if (movieProgress?.isFinished == true) {
                WatchStatus.COMPLETED.name
            } else {
                WatchStatus.WATCHING.name
            }

            db.execSQL(
                """
                INSERT OR IGNORE INTO movies_watch_history
                (filmId, ownerId, progress, status, duration, watchedAt, watchCount)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(filmId, ownerId, progress, status, duration, dateWatched, 1),
            )
        } catch (e: Exception) {
            errorLog("Error migrating movie progress for film $filmId: ${e.message}")
        }
    }

    private fun migrateEpisodeProgress(
        db: SupportSQLiteDatabase,
        filmId: String,
        ownerId: Int,
        episodesWatchedJson: String,
        dateWatched: Long,
    ) {
        try {
            val episodesWatched = parseEpisodesWatched(episodesWatchedJson)

            episodesWatched.forEach { episode ->
                val status = if (episode.isFinished) {
                    WatchStatus.COMPLETED.name
                } else {
                    WatchStatus.WATCHING.name
                }
                val seasonNumber = episode.seasonNumber ?: 1
                val episodeNumber = episode.episodeNumber ?: 1

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO series_watch_history
                    (filmId, ownerId, progress, duration, status, watchedAt, seasonNumber, episodeNumber)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        filmId,
                        ownerId,
                        episode.watchTime,
                        episode.durationTime,
                        status,
                        dateWatched,
                        seasonNumber,
                        episodeNumber,
                    ),
                )
            }
        } catch (e: Exception) {
            errorLog("Error migrating episode progress for film $filmId: ${e.message}")
        }
    }

    private fun parseEpisodesWatched(episodesWatchedJson: String): List<EpisodeWatchedData> {
        if (episodesWatchedJson.isEmpty() || episodesWatchedJson == "[]") {
            return emptyList()
        }

        return try {
            val jsonArray = JsonParser.parseString(episodesWatchedJson).asJsonArray
            jsonArray.map { element ->
                val obj = element.asJsonObject
                EpisodeWatchedData(
                    episodeId = obj.get("episodeId")?.asString ?: "",
                    seasonNumber = obj.get("seasonNumber")?.takeIf { !it.isJsonNull }?.asInt,
                    episodeNumber = obj.get("episodeNumber")?.takeIf { !it.isJsonNull }?.asInt,
                    watchTime = obj.get("watchTime")?.asLong ?: 0L,
                    durationTime = obj.get("durationTime")?.asLong ?: 0L,
                    isFinished = obj.get("isFinished")?.asBoolean ?: false,
                )
            }
        } catch (e: Exception) {
            errorLog("Error parsing episodes watched JSON: ${e.message}")
            emptyList()
        }
    }

    private fun migrateLibraryList(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_lists_new` (
                `listId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_library_lists_ownerId` ON `library_lists_new` (`ownerId`)",
        )

        db.execSQL(
            """
            INSERT INTO library_lists_new (listId, ownerId, name, description, createdAt, updatedAt)
            SELECT listId, ownerId, name, description, createdAt, updatedAt FROM library_lists
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE library_lists")
        db.execSQL("ALTER TABLE library_lists_new RENAME TO library_lists")
    }

    private fun migrateLibraryListItems(db: SupportSQLiteDatabase) {
        // Create new library_list_items table structure
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_list_items_new` (
                `itemId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `listId` INTEGER NOT NULL,
                `addedAt` INTEGER NOT NULL,
                FOREIGN KEY(`listId`) REFERENCES `library_lists`(`listId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )

        // Create composite unique index and other indices
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_library_list_items_filmId_listId` ON `library_list_items_new` (`filmId`, `listId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_library_list_items_filmId` ON `library_list_items_new` (`filmId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_library_list_items_listId` ON `library_list_items_new` (`listId`)",
        )

        // Migrate data from old table to new table
        val cursor = db.query("SELECT * FROM library_list_items")
        while (cursor.moveToNext()) {
            val oldItemId = cursor.getString(0)
            val filmJson = cursor.getString(1)

            try {
                val dbFilm = filmJson.toDBFilm()
                if (dbFilm.id.isNotEmpty()) {
                    // Insert into new table structure with ownerId
                    db.execSQL(
                        "INSERT OR IGNORE INTO library_list_items_new (filmId) VALUES (?)",
                        arrayOf(dbFilm.id),
                    )
                }
            } catch (e: Exception) {
                errorLog("Error migrating library list item $oldItemId: ${e.message}")
            }
        }
        cursor.close()

        // Drop old table and rename new table
        db.execSQL("DROP TABLE library_list_and_item_cross_ref")
        db.execSQL("DROP TABLE library_list_items")
        db.execSQL("ALTER TABLE library_list_items_new RENAME TO library_list_items")
    }

    private fun migrateWatchlist(db: SupportSQLiteDatabase) {
        // Create new watchlist table structure
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watchlist_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `addedAt` INTEGER NOT NULL,
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        // Create indices
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_watchlist_filmId_ownerId` ON `watchlist_new` (`filmId`, `ownerId`)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watchlist_filmId` ON `watchlist_new` (`filmId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watchlist_ownerId` ON `watchlist_new` (`ownerId`)")

        // Migrate data from old watchlist table to new table
        val cursor = db.query("SELECT id, film, ownerId, addedOn FROM watchlist")
        while (cursor.moveToNext()) {
            val oldId = cursor.getString(0)
            val filmJson = cursor.getString(1)
            val ownerId = cursor.getLong(2)
            val addedOn = cursor.getLong(3)

            try {
                val dbFilm = filmJson.toDBFilm()
                if (dbFilm.id.isNotEmpty()) {
                    // Insert into new table structure
                    db.execSQL(
                        "INSERT OR IGNORE INTO watchlist_new (filmId, ownerId, addedAt) VALUES (?, ?, ?)",
                        arrayOf(dbFilm.id, ownerId, addedOn),
                    )
                }
            } catch (e: Exception) {
                errorLog("Error migrating watchlist item $oldId: ${e.message}")
            }
        }
        cursor.close()

        // Drop old table and rename new table
        db.execSQL("DROP TABLE watchlist")
        db.execSQL("ALTER TABLE watchlist_new RENAME TO watchlist")
    }

    private fun processFilmsFromTable(
        db: SupportSQLiteDatabase,
        tableName: String,
    ) {
        val columnName = "film"
        val cursor =
            db.query("SELECT DISTINCT $columnName FROM $tableName WHERE $columnName IS NOT NULL AND $columnName != ''")
        while (cursor.moveToNext()) {
            val filmJson = cursor.getString(0)
            if (filmJson.isNotEmpty()) {
                try {
                    val dbFilm = filmJson.toDBFilm()
                    if (dbFilm.id.isNotEmpty() && !processedFilmIds.contains(dbFilm.id)) {
                        insertDBFilm(db, dbFilm)
                        processedFilmIds.add(dbFilm.id)
                    }
                } catch (e: Exception) {
                    errorLog("Error parsing film from $tableName: ${e.message}")
                }
            }
        }
        cursor.close()
    }

    private fun insertDBFilm(
        database: SupportSQLiteDatabase,
        dbFilm: DBFilm,
    ) {
        val currentTime = System.currentTimeMillis()

        database.execSQL(
            """
            INSERT OR IGNORE INTO films (
                id, providerId, imdbId, tmdbId, language, adult, title, runtime,
                backdropImage, posterImage, overview, homePage, releaseDate,
                logoImage, year, filmType, rating, customProperties, hasRecommendations, createdAt, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                dbFilm.id,
                dbFilm.providerId,
                dbFilm.imdbId,
                dbFilm.tmdbId,
                dbFilm.language,
                if (dbFilm.adult) 1 else 0,
                dbFilm.title,
                dbFilm.runtime,
                dbFilm.backdropImage,
                dbFilm.posterImage,
                dbFilm.overview,
                dbFilm.homePage,
                dbFilm.releaseDate,
                dbFilm.logoImage,
                dbFilm.year,
                dbFilm.filmType.name,
                dbFilm.rating,
                Json.encodeToString(dbFilm.customProperties),
                dbFilm.recommendations.size >= DB_FILM_VALID_RECOMMENDATIONS_COUNT,
                currentTime,
                currentTime,
            ),
        )
    }

    private fun migrateSearchHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `search_history_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `query` TEXT NOT NULL,
                    `ownerId` INTEGER NOT NULL,
                    `searchedOn` INTEGER NOT NULL,
                    FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent(),
        )

        db.execSQL("DROP INDEX IF EXISTS `index_search_history_query_ownerId`")

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query_ownerId` ON `search_history_new` (`query`, `ownerId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_search_history_ownerId` ON `search_history_new` (`ownerId`)",
        )

        db.execSQL(
            """
                INSERT INTO search_history_new (id, query, ownerId, searchedOn)
                SELECT id, query, ownerId, searchedOn FROM search_history
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE search_history")
        db.execSQL("ALTER TABLE search_history_new RENAME TO search_history")
    }

    // Data class to represent the parsed EpisodeWatched data during migration
    private data class EpisodeWatchedData(
        val episodeId: String,
        val seasonNumber: Int?,
        val episodeNumber: Int?,
        val watchTime: Long,
        val durationTime: Long,
        val isFinished: Boolean,
    )

    private object DBFilmMigrator {
        fun String.toDBFilm(): DBFilm {
            val json = JsonParser.parseString(this)

            runCatching {
                json.migrateToSchema4()
            }

            runCatching {
                json.migrateToSchema8()
            }

            return fromJson<DBFilm>(json)
        }

        private fun JsonElement.migrateToSchema8() {
            val json = asJsonObject

            if (json.has("providerName")) {
                json.addProperty("providerId", json.get("providerName").asString)
                json.remove("providerName")
            }

            if (json.has("recommendations")) {
                val recommendations = json.get("recommendations").asJsonArray

                recommendations.forEach { recommendation ->
                    with(recommendation.asJsonObject) {
                        if (has("providerName")) {
                            addProperty("providerId", get("providerName").asString)
                            remove("providerName")
                        }
                    }
                }
            }
        }

        private fun JsonElement.migrateToSchema4() {
            val json = asJsonObject

            val releaseDate = json.get("dateReleased").asString
            json.addProperty("releaseDate", releaseDate)

            val tmdbId = json.get("id").asInt
            json.addProperty("tmdbId", tmdbId)
            json.addProperty("id", "")

            val runtime = json.get("runtime").asJsonPrimitive
            if (!runtime.isJsonNull && runtime.isString) {
                json.remove("runtime")
            }

            json.add("recommendations", JsonArray())
        }
    }
}
