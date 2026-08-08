package com.flixclusive.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flixclusive.core.database.migration.Schema10to11
import com.flixclusive.core.database.migration.Schema11to12
import com.flixclusive.core.database.migration.Schema12to13
import com.flixclusive.core.database.migration.Schema13to14
import com.flixclusive.core.database.migration.Schema14to15
import com.flixclusive.core.database.migration.Schema15to16
import com.flixclusive.core.database.migration.Schema16to17
import com.flixclusive.core.database.migration.Schema17to18
import com.flixclusive.core.database.migration.Schema18to19
import com.flixclusive.core.database.migration.Schema19to20
import com.flixclusive.core.database.migration.Schema1to2
import com.flixclusive.core.database.migration.Schema20to21
import com.flixclusive.core.database.migration.Schema21to22
import com.flixclusive.core.database.migration.Schema22to23
import com.flixclusive.core.database.migration.Schema2to3
import com.flixclusive.core.database.migration.Schema3to4
import com.flixclusive.core.database.migration.Schema4to5
import com.flixclusive.core.database.migration.Schema5to6
import com.flixclusive.core.database.migration.Schema6to7
import com.flixclusive.core.database.migration.Schema7to8
import com.flixclusive.core.database.migration.Schema8to9
import com.flixclusive.core.database.migration.Schema9to10
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.assertions.isEqualTo
import strikt.api.expectThat
import java.io.IOException

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
class DBMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        helper.createDatabase(TEST_DB, 2)

        helper.runMigrationsAndValidate(
            name = TEST_DB,
            version = 23,
            validateDroppedTables = true,
            Schema1to2,
            Schema2to3,
            Schema3to4,
            Schema4to5,
            Schema5to6,
            Schema6to7,
            Schema7to8,
            Schema8to9,
            Schema9to10(context),
            Schema10to11(context),
            Schema11to12,
            Schema12to13,
            Schema13to14,
            Schema14to15,
            Schema15to16,
            Schema16to17,
            Schema17to18,
            Schema18to19,
            Schema19to20,
            Schema20to21,
            Schema21to22,
            Schema22to23,
        )
    }

    /**
     * The empty-database run above never exercises the part of [Schema22to23] most likely to go
     * wrong: collapsing rows that already violate the unique index it then creates. Seeds the
     * duplicates a pre-23 install can be carrying and checks the right one survives with its chunks.
     */
    @Test
    @Throws(IOException::class)
    fun migrateShouldCollapseDuplicateDownloadsKeepingTheFurthestAlongRow() {
        helper.createDatabase(TEST_DB, 22).use { db ->
            db.insertDownloadItem(id = "behind", mediaId = "m1", streamBytesDownloaded = 10)
            db.insertDownloadItem(id = "ahead", mediaId = "m1", streamBytesDownloaded = 900)
            // A different title must survive untouched alongside them.
            db.insertDownloadItem(id = "other", mediaId = "m2", streamBytesDownloaded = 5)
            db.execSQL(
                """
                INSERT INTO `download_chunks` (`downloadItemId`, `chunkIndex`, `rangeStart`, `rangeEnd`,
                    `bytesDownloaded`, `status`)
                VALUES ('behind', 0, 0, 99, 10, 'DOWNLOADING'), ('ahead', 0, 0, 999, 900, 'DOWNLOADING')
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 23, true, Schema22to23)

        db.query("SELECT `id`, `dedupeKey` FROM `download_items` ORDER BY `id`").use { cursor ->
            val rows = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
            }
            expectThat(rows).isEqualTo(listOf("ahead" to "m1|-1|-1", "other" to "m2|-1|-1"))
        }

        // The loser's chunks go with it — Room disables foreign keys during a migration, so the
        // CASCADE never fires and they would otherwise be orphaned.
        db.query("SELECT `downloadItemId` FROM `download_chunks`").use { cursor ->
            val owners = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
            expectThat(owners).isEqualTo(listOf("ahead"))
        }
    }

    private fun SupportSQLiteDatabase.insertDownloadItem(
        id: String,
        mediaId: String,
        streamBytesDownloaded: Long,
    ) = execSQL(
        """
        INSERT INTO `download_items` (`id`, `ownerId`, `mediaId`, `mediaTitle`, `mediaType`, `state`,
            `isHlsStream`, `streamBytesDownloaded`, `streamTotalBytes`, `downloadBytesPerSecond`,
            `downloadedSubtitlesCount`, `totalSubtitlesCount`, `createdAt`, `updatedAt`)
        VALUES ('$id', 'owner-1', '$mediaId', 'Title', 'MOVIE', 'DOWNLOADING_STREAM',
            0, $streamBytesDownloaded, 1000, 0, 0, 0, 1, 1)
        """.trimIndent(),
    )
}
