package com.flixclusive.core.database

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flixclusive.core.database.migration.Schema10to11
import com.flixclusive.core.database.migration.Schema1to2
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

    @Throws(IOException::class)
    private fun testMigrate(
        initialVersion: Int,
        migrateVersion: Int,
        vararg migration: Migration,
    ) {
        helper.createDatabase(TEST_DB, initialVersion)

        helper.runMigrationsAndValidate(
            name = TEST_DB,
            version = migrateVersion,
            validateDroppedTables = true,
            *migration,
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        testMigrate(
            initialVersion = 2,
            migrateVersion = 3,
            Schema1to2,
            Schema2to3,
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        testMigrate(
            initialVersion = 3,
            migrateVersion = 4,
            Schema1to2,
            Schema2to3,
            Schema3to4,
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        testMigrate(
            initialVersion = 4,
            migrateVersion = 5,
            Schema1to2,
            Schema2to3,
            Schema3to4,
            Schema4to5,
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        testMigrate(
            initialVersion = 5,
            migrateVersion = 6,
            Schema1to2,
            Schema2to3,
            Schema3to4,
            Schema4to5,
            Schema5to6,
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate6to7() {
        testMigrate(
            initialVersion = 6,
            migrateVersion = 7,
            Schema1to2,
            Schema2to3,
            Schema3to4,
            Schema4to5,
            Schema5to6,
            Schema6to7,
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        testMigrate(
            initialVersion = 7,
            migrateVersion = 8,
            Schema1to2,
            Schema2to3,
            Schema3to4,
            Schema4to5,
            Schema5to6,
            Schema6to7,
            Schema7to8,
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        testMigrate(
            initialVersion = 8,
            migrateVersion = 9,
            Schema1to2,
            Schema2to3,
            Schema3to4,
            Schema4to5,
            Schema5to6,
            Schema6to7,
            Schema7to8,
            Schema8to9,
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate9To10() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testMigrate(
            initialVersion = 9,
            migrateVersion = 10,
            Schema1to2,
            Schema2to3,
            Schema3to4,
            Schema4to5,
            Schema5to6,
            Schema6to7,
            Schema7to8,
            Schema8to9,
            Schema9to10(context),
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate10To11() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testMigrate(
            initialVersion = 10,
            migrateVersion = 11,
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
        )
    }
}
