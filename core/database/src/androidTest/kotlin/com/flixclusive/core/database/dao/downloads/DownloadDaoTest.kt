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
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.util.Date

@RunWith(AndroidJUnit4::class)
class DownloadDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var downloadItemDao: DownloadItemDao
    private lateinit var downloadChunkDao: DownloadChunkDao

    private val testItem = DownloadItem(
        ownerId = "owner-1",
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
    fun insertShouldPersistItemAndGetShouldReturnInsertedItem() =
        runTest {
            downloadItemDao.insert(testItem)

            val result = downloadItemDao.get(testItem.id)

            expectThat(result?.mediaTitle).isEqualTo("Test Movie")
            expectThat(result?.state).isEqualTo(DownloadItemState.QUEUED)
        }

    @Test
    fun updateStateShouldPersistStateAndPhase() =
        runTest {
            downloadItemDao.insert(testItem)

            downloadItemDao.updateState(testItem.id, DownloadItemState.PAUSED, DownloadPhase.STREAM, Date())

            val result = downloadItemDao.get(testItem.id)
            expectThat(result?.state).isEqualTo(DownloadItemState.PAUSED)
            expectThat(result?.phase).isEqualTo(DownloadPhase.STREAM)
        }

    @Test
    fun updateStreamProgressShouldPersistByteCountsWithoutTouchingOtherFields() =
        runTest {
            downloadItemDao.insert(testItem)

            downloadItemDao.updateStreamProgress(
                testItem.id,
                bytesDownloaded = 512,
                totalBytes = 1024,
                updatedAt = Date(),
            )

            val result = downloadItemDao.get(testItem.id)
            expectThat(result?.streamBytesDownloaded).isEqualTo(512)
            expectThat(result?.streamTotalBytes).isEqualTo(1024)
            expectThat(result?.mediaTitle).isEqualTo("Test Movie")
        }

    @Test
    fun updateSourceShouldAlwaysWriteSourceUrlAndIsHlsStreamTogetherAndResetProgress() =
        runTest {
            downloadItemDao.insert(testItem)
            downloadItemDao.updateStreamProgress(testItem.id, bytesDownloaded = 512, totalBytes = 1024, Date())

            downloadItemDao.updateSource(testItem.id, "https://example.com/video.m3u8", isHlsStream = true, Date())

            val result = downloadItemDao.get(testItem.id)
            expectThat(result?.sourceUrl).isEqualTo("https://example.com/video.m3u8")
            expectThat(result?.isHlsStream).isTrue()
            expectThat(result?.streamBytesDownloaded).isEqualTo(0)
            expectThat(result?.streamTotalBytes).isEqualTo(0)
        }

    @Test
    fun updateStreamFilePathShouldPersistPath() =
        runTest {
            downloadItemDao.insert(testItem)

            downloadItemDao.updateStreamFilePath(testItem.id, "content://tree/document/video.mp4", Date())

            val result = downloadItemDao.get(testItem.id)
            expectThat(result?.streamFilePath).isEqualTo("content://tree/document/video.mp4")
        }

    @Test
    fun incrementDownloadedSubtitlesCountShouldAccumulate() =
        runTest {
            downloadItemDao.insert(testItem)
            downloadItemDao.setTotalSubtitlesCount(testItem.id, 3, Date())

            downloadItemDao.incrementDownloadedSubtitlesCount(testItem.id, Date())
            downloadItemDao.incrementDownloadedSubtitlesCount(testItem.id, Date())

            val result = downloadItemDao.get(testItem.id)
            expectThat(result?.totalSubtitlesCount).isEqualTo(3)
            expectThat(result?.downloadedSubtitlesCount).isEqualTo(2)
        }

    @Test
    fun insertAllChunksShouldBeRetrievableOrderedByChunkIndex() =
        runTest {
            downloadItemDao.insert(testItem)
            val chunks = listOf(
                DownloadChunk(downloadItemId = testItem.id, chunkIndex = 1, rangeStart = 500, rangeEnd = 999),
                DownloadChunk(downloadItemId = testItem.id, chunkIndex = 0, rangeStart = 0, rangeEnd = 499),
            )

            downloadChunkDao.insertAll(chunks)

            val result = downloadChunkDao.getChunksForItem(testItem.id)
            expectThat(result).hasSize(2)
            expectThat(result[0].chunkIndex).isEqualTo(0)
            expectThat(result[1].chunkIndex).isEqualTo(1)
        }

    @Test
    fun updateProgressShouldPersistChunkBytesAndStatus() =
        runTest {
            downloadItemDao.insert(testItem)
            downloadChunkDao.insertAll(
                listOf(DownloadChunk(downloadItemId = testItem.id, chunkIndex = 0, rangeStart = 0, rangeEnd = 999))
            )
            val chunkId = downloadChunkDao.getChunksForItem(testItem.id).first().id

            downloadChunkDao.updateProgress(chunkId, bytesDownloaded = 1000, status = DownloadChunkStatus.COMPLETED)

            val result = downloadChunkDao.getChunksForItem(testItem.id).first()
            expectThat(result.bytesDownloaded).isEqualTo(1000)
            expectThat(result.status).isEqualTo(DownloadChunkStatus.COMPLETED)
        }

    @Test
    fun deletingDownloadItemShouldCascadeDeleteItsChunks() =
        runTest {
            downloadItemDao.insert(testItem)
            downloadChunkDao.insertAll(
                listOf(DownloadChunk(downloadItemId = testItem.id, chunkIndex = 0, rangeStart = 0, rangeEnd = 999))
            )

            downloadItemDao.delete(testItem.id)

            expectThat(downloadChunkDao.getChunksForItem(testItem.id)).isEmpty()
            expectThat(downloadItemDao.get(testItem.id)).isNull()
        }

    @Test
    fun getOldestByStateShouldReturnEarliestQueuedItemFirst() =
        runTest {
            downloadItemDao.insert(testItem)
            val second = testItem.copy(id = "item-2", episodeNumber = 2)
            downloadItemDao.insert(second)

            val result = downloadItemDao.getOldestByState(DownloadItemState.QUEUED)

            expectThat(result?.id).isEqualTo(testItem.id)
        }

    @Test
    fun getBatchShouldReturnItemsForSameMediaAndSeasonOrderedByEpisodeNumber() =
        runTest {
            val showId = "show-1"
            downloadItemDao.insert(
                testItem.copy(
                    id = "item-1",
                    mediaId = showId,
                    mediaType = MediaType.SHOW,
                    seasonNumber = 1,
                    episodeNumber = 2,
                )
            )
            downloadItemDao.insert(
                testItem.copy(
                    id = "item-2",
                    mediaId = showId,
                    mediaType = MediaType.SHOW,
                    seasonNumber = 1,
                    episodeNumber = 1,
                )
            )
            downloadItemDao.insert(
                testItem.copy(
                    id = "item-3",
                    mediaId = showId,
                    mediaType = MediaType.SHOW,
                    seasonNumber = 2,
                    episodeNumber = 1,
                )
            )

            val batch = downloadItemDao.getBatch(showId, 1)

            expectThat(batch).hasSize(2)
            expectThat(batch[0].episodeNumber).isEqualTo(1)
            expectThat(batch[1].episodeNumber).isEqualTo(2)
        }
}
