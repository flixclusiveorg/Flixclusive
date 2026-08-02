package com.flixclusive.core.database.dao.downloads

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.model.media.common.MediaType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNull
import java.util.Date

@RunWith(AndroidJUnit4::class)
class DownloadDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var downloadItemDao: DownloadItemDao
    private lateinit var downloadChunkDao: DownloadChunkDao

    private val testItem = DownloadItem(
        mediaId = "media-1",
        mediaTitle = "Test Movie",
        mediaType = MediaType.MOVIE,
    )

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = DatabaseTestDefaults.createDatabase(context)
        downloadItemDao = database.downloadItemDao()
        downloadChunkDao = database.downloadChunkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertShouldAssignIdAndGetShouldReturnInsertedItem() =
        runTest {
            val id = downloadItemDao.insert(testItem)

            val result = downloadItemDao.get(id)

            expectThat(result?.mediaTitle).isEqualTo("Test Movie")
            expectThat(result?.state).isEqualTo(DownloadItemState.QUEUED)
        }

    @Test
    fun updateStateShouldPersistStateAndPhase() =
        runTest {
            val id = downloadItemDao.insert(testItem)

            downloadItemDao.updateState(id, DownloadItemState.PAUSED, DownloadPhase.STREAM, Date())

            val result = downloadItemDao.get(id)
            expectThat(result?.state).isEqualTo(DownloadItemState.PAUSED)
            expectThat(result?.phase).isEqualTo(DownloadPhase.STREAM)
        }

    @Test
    fun updateStreamProgressShouldPersistByteCountsWithoutTouchingOtherFields() =
        runTest {
            val id = downloadItemDao.insert(testItem)

            downloadItemDao.updateStreamProgress(id, bytesDownloaded = 512, totalBytes = 1024, updatedAt = Date())

            val result = downloadItemDao.get(id)
            expectThat(result?.streamBytesDownloaded).isEqualTo(512)
            expectThat(result?.streamTotalBytes).isEqualTo(1024)
            expectThat(result?.mediaTitle).isEqualTo("Test Movie")
        }

    @Test
    fun insertAllChunksShouldBeRetrievableOrderedByChunkIndex() =
        runTest {
            val itemId = downloadItemDao.insert(testItem)
            val chunks = listOf(
                DownloadChunk(downloadItemId = itemId, chunkIndex = 1, rangeStart = 500, rangeEnd = 999),
                DownloadChunk(downloadItemId = itemId, chunkIndex = 0, rangeStart = 0, rangeEnd = 499),
            )

            downloadChunkDao.insertAll(chunks)

            val result = downloadChunkDao.getChunksForItem(itemId)
            expectThat(result).hasSize(2)
            expectThat(result[0].chunkIndex).isEqualTo(0)
            expectThat(result[1].chunkIndex).isEqualTo(1)
        }

    @Test
    fun updateProgressShouldPersistChunkBytesAndStatus() =
        runTest {
            val itemId = downloadItemDao.insert(testItem)
            downloadChunkDao.insertAll(
                listOf(DownloadChunk(downloadItemId = itemId, chunkIndex = 0, rangeStart = 0, rangeEnd = 999))
            )
            val chunkId = downloadChunkDao.getChunksForItem(itemId).first().id

            downloadChunkDao.updateProgress(chunkId, bytesDownloaded = 1000, status = DownloadChunkStatus.COMPLETED)

            val result = downloadChunkDao.getChunksForItem(itemId).first()
            expectThat(result.bytesDownloaded).isEqualTo(1000)
            expectThat(result.status).isEqualTo(DownloadChunkStatus.COMPLETED)
        }

    @Test
    fun deletingDownloadItemShouldCascadeDeleteItsChunks() =
        runTest {
            val itemId = downloadItemDao.insert(testItem)
            downloadChunkDao.insertAll(
                listOf(DownloadChunk(downloadItemId = itemId, chunkIndex = 0, rangeStart = 0, rangeEnd = 999))
            )

            downloadItemDao.delete(itemId)

            expectThat(downloadChunkDao.getChunksForItem(itemId)).isEmpty()
            expectThat(downloadItemDao.get(itemId)).isNull()
        }

    @Test
    fun getOldestByStateShouldReturnEarliestQueuedItemFirst() =
        runTest {
            val first = downloadItemDao.insert(testItem)
            val second = downloadItemDao.insert(testItem.copy(episodeNumber = 2))

            val result = downloadItemDao.getOldestByState(DownloadItemState.QUEUED)

            expectThat(result?.id).isEqualTo(first)
            expectThat(second).isNotEqualTo(first)
        }

    @Test
    fun getBatchShouldReturnItemsForSameMediaAndSeasonOrderedByEpisodeNumber() =
        runTest {
            val showId = "show-1"
            downloadItemDao.insert(
                testItem.copy(mediaId = showId, mediaType = MediaType.SHOW, seasonNumber = 1, episodeNumber = 2)
            )
            downloadItemDao.insert(
                testItem.copy(mediaId = showId, mediaType = MediaType.SHOW, seasonNumber = 1, episodeNumber = 1)
            )
            downloadItemDao.insert(
                testItem.copy(mediaId = showId, mediaType = MediaType.SHOW, seasonNumber = 2, episodeNumber = 1)
            )

            val batch = downloadItemDao.getBatch(showId, 1)

            expectThat(batch).hasSize(2)
            expectThat(batch[0].episodeNumber).isEqualTo(1)
            expectThat(batch[1].episodeNumber).isEqualTo(2)
        }
}
