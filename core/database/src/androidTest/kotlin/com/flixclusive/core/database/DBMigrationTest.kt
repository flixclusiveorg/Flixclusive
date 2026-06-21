package com.flixclusive.core.database

import androidx.room.testing.MigrationTestHelper
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

    @Test
    @Throws(IOException::class)
    fun migrate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        helper.createDatabase(TEST_DB, 2)

        helper.runMigrationsAndValidate(
            name = TEST_DB,
            version = 19,
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
        )
    }
}
