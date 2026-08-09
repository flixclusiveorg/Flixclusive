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

/** 6 Sep 2010, a perfectly ordinary release date, in epoch milliseconds. */
private const val TRUE_MILLIS = 1_283_731_200_000L

/** 1 Jan 1950 — negative, to cover releases that predate the epoch. */
private const val TRUE_MILLIS_1950 = -631_152_000_000L

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
            version = 22,
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
        )
    }

    /**
     * The values a pre-22 install is actually carrying: release dates multiplied by a thousand on
     * every write, so what sits in a milliseconds column is really microseconds — and more than that
     * for a title that was re-saved from what the database already held.
     */
    @Test
    @Throws(IOException::class)
    fun migrateShouldRescaleInflatedReleaseDatesBackToMilliseconds() {
        helper.createDatabase(TEST_DB, 21).use { db ->
            db.insertMedia(id = "once", releaseDate = TRUE_MILLIS * 1_000)
            // Survived two save/read cycles before the fix landed.
            db.insertMedia(id = "twice", releaseDate = TRUE_MILLIS * 1_000_000)
            // Pre-1970 releases inflate downwards; a one-sided upper-bound test would skip them.
            db.insertMedia(id = "negative", releaseDate = TRUE_MILLIS_1950 * 1_000)
            // Anything already sane must come through byte-for-byte.
            db.insertMedia(id = "correct", releaseDate = TRUE_MILLIS)
            db.insertMedia(id = "absent", releaseDate = null)
            // The old conversion had no null path, so "no release date" was written as epoch 0.
            db.insertMedia(id = "zero", releaseDate = 0)
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 22, true, Schema21to22)

        db.query("SELECT `id`, `releaseDate` FROM `media` ORDER BY `id`").use { cursor ->
            val rows = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0) to if (cursor.isNull(1)) null else cursor.getLong(1))
                }
            }

            expectThat(rows).isEqualTo(
                listOf(
                    "absent" to null,
                    "correct" to TRUE_MILLIS,
                    "negative" to TRUE_MILLIS_1950,
                    "once" to TRUE_MILLIS,
                    "twice" to TRUE_MILLIS,
                    "zero" to null,
                ),
            )
        }
    }

    private fun SupportSQLiteDatabase.insertMedia(
        id: String,
        releaseDate: Long?,
    ) = execSQL(
        """
        INSERT INTO `media` (`id`, `title`, `providerId`, `adult`, `type`, `releaseDate`,
            `createdAt`, `updatedAt`)
        VALUES ('$id', 'Title', 'provider-1', 0, 'MOVIE', ${releaseDate ?: "NULL"}, 1, 1)
        """.trimIndent(),
    )
}
