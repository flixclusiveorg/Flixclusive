package com.flixclusive.core.database.migration

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.util.UUID

/**
 * Migration from schema version 10 to 11.
 *
 * ## Changes
 * - [User] primary key `userId` migrated from INTEGER AUTOINCREMENT to UUID TEXT.
 * - All user-scoped tables migrated from `ownerId/userId: INTEGER` to `TEXT`.
 * - Provider tables updated for multi-user support:
 *   - `repositories` primary key becomes (`url`, `userId`).
 *   - `installed_providers` primary key becomes (`id`, `ownerId`).
 *   - FK from `installed_providers` → `repositories` becomes (`repositoryUrl`, `ownerId`) → (`url`, `userId`).
 */
internal class Schema10to11(private val context: Context) : Migration(startVersion = 10, endVersion = 11) {
    private companion object {
        const val USER_PREFERENCE_FILE_PREFIX = "user-preferences-"
        const val USER_PREFERENCE_FILE_SUFFIX = ".preferences_pb"
    }

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")
        db.beginTransaction()

        try {
            createUserIdMap(db)

            migrateUserTable(db)
            migrateSearchHistory(db)
            migrateLibraryLists(db)
            migrateMoviesWatchHistory(db)
            migrateSeriesWatchHistory(db)
            migrateRepositories(db)
            migrateInstalledProviders(db)

            val userIdMap = readUserIdMap(db)
            migrateLegacyUserPreferenceFiles(userIdMap)

            db.execSQL("DROP TABLE IF EXISTS `user_id_map`")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private fun createUserIdMap(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_id_map` (
                `oldUserId` INTEGER NOT NULL,
                `newUserId` TEXT NOT NULL,
                PRIMARY KEY(`oldUserId`)
            )
            """.trimIndent(),
        )

        val cursor = db.query("SELECT `userId` FROM `User`")
        cursor.use {
            while (it.moveToNext()) {
                val oldUserId = it.getInt(0)
                val newUserId = UUID.randomUUID().toString()

                db.execSQL(
                    "INSERT OR REPLACE INTO `user_id_map` (`oldUserId`, `newUserId`) VALUES (?, ?)",
                    arrayOf<Any>(oldUserId, newUserId),
                )
            }
        }
    }

    private fun readUserIdMap(db: SupportSQLiteDatabase): Map<Int, String> {
        val result = mutableMapOf<Int, String>()

        db.query("SELECT `oldUserId`, `newUserId` FROM `user_id_map`").use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getInt(0)] = cursor.getString(1)
            }
        }

        return result
    }

    private fun migrateLegacyUserPreferenceFiles(userIdMap: Map<Int, String>) {
        val datastoreUsersDir = File(context.filesDir, "datastore/users")
        if (!datastoreUsersDir.exists() || !datastoreUsersDir.isDirectory) return

        userIdMap.forEach { (legacyUserId, uuidUserId) ->
            val legacyFile = File(
                datastoreUsersDir,
                "$USER_PREFERENCE_FILE_PREFIX$legacyUserId$USER_PREFERENCE_FILE_SUFFIX",
            )
            if (!legacyFile.exists() || !legacyFile.isFile) return@forEach

            val newFile = File(
                datastoreUsersDir,
                "$USER_PREFERENCE_FILE_PREFIX$uuidUserId$USER_PREFERENCE_FILE_SUFFIX",
            )
            if (newFile.exists()) return@forEach

            newFile.parentFile?.mkdirs()
            if (legacyFile.renameTo(newFile)) return@forEach

            legacyFile.copyTo(newFile, overwrite = true)
            legacyFile.delete()
        }
    }

    private fun migrateUserTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `User_new` (
                `userId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `image` INTEGER NOT NULL,
                `pin` TEXT,
                `pinHint` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `legacyId` INTEGER NOT NULL,
                PRIMARY KEY(`userId`)
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `User_new` (`userId`, `name`, `image`, `pin`, `pinHint`, `createdAt`, `updatedAt`, `legacyId`)
            SELECT map.`newUserId`, u.`name`, u.`image`, u.`pin`, u.`pinHint`, u.`createdAt`, u.`updatedAt`, u.`userId`
            FROM `User` u
            INNER JOIN `user_id_map` map ON map.`oldUserId` = u.`userId`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `User`")
        db.execSQL("ALTER TABLE `User_new` RENAME TO `User`")
    }

    private fun migrateSearchHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `search_history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `query` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `search_history_new` (`id`, `query`, `ownerId`, `createdAt`, `updatedAt`)
            SELECT sh.`id`, sh.`query`, map.`newUserId`, sh.`createdAt`, sh.`updatedAt`
            FROM `search_history` sh
            INNER JOIN `user_id_map` map ON map.`oldUserId` = sh.`ownerId`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `search_history`")
        db.execSQL("ALTER TABLE `search_history_new` RENAME TO `search_history`")

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query_ownerId` ON `search_history` (`query`, `ownerId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_search_history_ownerId` ON `search_history` (`ownerId`)",
        )
    }

    private fun migrateLibraryLists(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_lists_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `listType` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `library_lists_new` (`id`, `ownerId`, `name`, `description`, `listType`, `createdAt`, `updatedAt`)
            SELECT l.`id`, map.`newUserId`, l.`name`, l.`description`, l.`listType`, l.`createdAt`, l.`updatedAt`
            FROM `library_lists` l
            INNER JOIN `user_id_map` map ON map.`oldUserId` = l.`ownerId`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `library_lists`")
        db.execSQL("ALTER TABLE `library_lists_new` RENAME TO `library_lists`")

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_library_lists_ownerId` ON `library_lists` (`ownerId`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_library_lists_ownerId_name` ON `library_lists` (`ownerId`, `name`)",
        )
    }

    private fun migrateMoviesWatchHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `movies_watch_history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `progress` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `duration` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `movies_watch_history_new` (`id`, `filmId`, `ownerId`, `progress`, `status`, `duration`, `createdAt`, `updatedAt`)
            SELECT m.`id`, m.`filmId`, map.`newUserId`, m.`progress`, m.`status`, m.`duration`, m.`createdAt`, m.`updatedAt`
            FROM `movies_watch_history` m
            INNER JOIN `user_id_map` map ON map.`oldUserId` = m.`ownerId`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `movies_watch_history`")
        db.execSQL("ALTER TABLE `movies_watch_history_new` RENAME TO `movies_watch_history`")

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_movies_watch_history_filmId_ownerId` ON `movies_watch_history` (`filmId`, `ownerId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_movies_watch_history_filmId` ON `movies_watch_history` (`filmId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_movies_watch_history_ownerId` ON `movies_watch_history` (`ownerId`)",
        )
    }

    private fun migrateSeriesWatchHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `series_watch_history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `filmId` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `progress` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `duration` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `seasonNumber` INTEGER NOT NULL,
                `episodeNumber` INTEGER NOT NULL,
                FOREIGN KEY(`filmId`) REFERENCES `films`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `series_watch_history_new` (
                `id`, `filmId`, `ownerId`, `progress`, `status`, `duration`,
                `createdAt`, `updatedAt`, `seasonNumber`, `episodeNumber`
            )
            SELECT s.`id`, s.`filmId`, map.`newUserId`, s.`progress`, s.`status`, s.`duration`,
                s.`createdAt`, s.`updatedAt`, s.`seasonNumber`, s.`episodeNumber`
            FROM `series_watch_history` s
            INNER JOIN `user_id_map` map ON map.`oldUserId` = s.`ownerId`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `series_watch_history`")
        db.execSQL("ALTER TABLE `series_watch_history_new` RENAME TO `series_watch_history`")

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

    private fun migrateRepositories(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `repositories_new` (
                `url` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `owner` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `rawLinkFormat` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`url`, `userId`),
                FOREIGN KEY(`userId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `repositories_new` (`url`, `userId`, `owner`, `name`, `rawLinkFormat`, `createdAt`, `updatedAt`)
            SELECT r.`url`, map.`newUserId`, r.`owner`, r.`name`, r.`rawLinkFormat`, r.`createdAt`, r.`updatedAt`
            FROM `repositories` r
            INNER JOIN `user_id_map` map ON map.`oldUserId` = r.`userId`
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO `repositories_new` (`url`, `userId`, `owner`, `name`, `rawLinkFormat`, `createdAt`, `updatedAt`)
            SELECT r.`url`, map.`newUserId`, r.`owner`, r.`name`, r.`rawLinkFormat`, r.`createdAt`, r.`updatedAt`
            FROM `installed_providers` p
            INNER JOIN `repositories` r ON r.`url` = p.`repositoryUrl`
            INNER JOIN `user_id_map` map ON map.`oldUserId` = p.`ownerId`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `repositories`")
        db.execSQL("ALTER TABLE `repositories_new` RENAME TO `repositories`")

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_repositories_userId` ON `repositories` (`userId`)",
        )
    }

    private fun migrateInstalledProviders(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `installed_providers_new` (
                `id` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `repositoryUrl` TEXT NOT NULL,
                `filePath` TEXT NOT NULL,
                `sortOrder` REAL NOT NULL,
                `isEnabled` INTEGER NOT NULL,
                `isDebug` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`, `ownerId`),
                FOREIGN KEY(`repositoryUrl`, `ownerId`) REFERENCES `repositories`(`url`, `userId`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`ownerId`) REFERENCES `User`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `installed_providers_new` (
                `id`, `ownerId`, `repositoryUrl`, `filePath`, `sortOrder`,
                `isEnabled`, `isDebug`, `createdAt`, `updatedAt`
            )
            SELECT p.`id`, map.`newUserId`, p.`repositoryUrl`, p.`filePath`, p.`sortOrder`,
                p.`isEnabled`, p.`isDebug`, p.`createdAt`, p.`updatedAt`
            FROM `installed_providers` p
            INNER JOIN `user_id_map` map ON map.`oldUserId` = p.`ownerId`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `installed_providers`")
        db.execSQL("ALTER TABLE `installed_providers_new` RENAME TO `installed_providers`")

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_repositoryUrl` ON `installed_providers` (`repositoryUrl`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_ownerId` ON `installed_providers` (`ownerId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_repositoryUrl_ownerId` ON `installed_providers` (`repositoryUrl`, `ownerId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_sortOrder` ON `installed_providers` (`sortOrder`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_installed_providers_isEnabled` ON `installed_providers` (`isEnabled`)",
        )
    }
}
