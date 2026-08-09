package com.flixclusive.core.database.dao.watched

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.model.media.common.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import java.util.Date

/**
 * Pins down how a movie progress row behaves when the media it points at isn't in the database —
 * the shape behind "Relationship item 'media' was expected to be NON-NULL but is NULL".
 */
@RunWith(AndroidJUnit4::class)
class MovieProgressDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var movieProgressDao: MovieProgressDao

    private val ownerId = DatabaseTestDefaults.TEST_USER_ID

    private fun media(id: String = "media-1") = DBMedia(
        id = id,
        title = "Test Movie",
        providerId = "",
        adult = false,
        type = MediaType.MOVIE,
        overview = null,
        posterImage = null,
        language = null,
        rating = null,
        backdropImage = null,
        releaseDate = null,
    )

    private fun progress(mediaId: String = "media-1") = MovieProgress(
        mediaId = mediaId,
        ownerId = ownerId,
        progress = 120_000,
        status = WatchStatus.WATCHING,
        duration = 600_000,
        updatedAt = Date(),
    )

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = DatabaseTestDefaults.createDatabase(context)
        movieProgressDao = database.movieProgressDao()

        runTest { database.userDao().insert(DatabaseTestDefaults.getUser()) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertWithMediaShouldBeReadableWithItsMetadata() =
        runTest {
            movieProgressDao.insert(item = progress(), media = media())

            val all = movieProgressDao.getAll(ownerId)

            expectThat(all).hasSize(1)
            expectThat(all.first().media.id).isEqualTo("media-1")
        }

    @Test
    fun insertWithoutMediaShouldBeRejectedByTheForeignKey() =
        runTest {
            var rejected = false
            try {
                movieProgressDao.insert(item = progress(mediaId = "missing-media"), media = null)
            } catch (_: Exception) {
                rejected = true
            }

            expectThat(rejected).isEqualTo(true)
        }

    @Test
    fun readsShouldSkipAHistoryRowWhoseMediaIsMissingInsteadOfThrowing() =
        runTest {
            // The crash: MovieProgressWithMetadata declares its media non-null, so a row without one
            // doesn't come back empty — it throws and takes the whole list with it. The foreign key
            // above stops new ones, but it isn't enforced during migrations and can't retroactively
            // reject rows written before it existed, so reads have to cope with what's already there.
            movieProgressDao.insert(item = progress(), media = media())
            database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = OFF")
            database.openHelper.writableDatabase.execSQL(
                """
                INSERT INTO movies_watch_history
                    (mediaId, ownerId, progress, status, duration, createdAt, updatedAt)
                VALUES ('ghost-media', '$ownerId', 1000, 'WATCHING', 5000, 1, 1)
                """.trimIndent(),
            )

            val all = movieProgressDao.getAll(ownerId)

            expectThat(all).hasSize(1)
            expectThat(all.first().media.id).isEqualTo("media-1")

            // The raw sorted flow is the one the crash actually came through, and it builds its SQL
            // separately — so it needs asserting in its own right, not by proxy.
            val observed = movieProgressDao
                .getAllAsFlow(ownerId = ownerId, column = "createdAt", ascending = false)
                .first()

            expectThat(observed).hasSize(1)
            expectThat(observed.first().media.id).isEqualTo("media-1")
        }
}
