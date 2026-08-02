package com.flixclusive.domain.downloads.usecase.impl

import android.net.Uri
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.domain.downloads.usecase.GetDownloadDirectoryUseCase
import com.flixclusive.model.media.common.MediaType
import com.hippo.unifile.UniFile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class GetCompletedDownloadFileUseCaseImplTest {
    private lateinit var getDownloadDirectoryUseCase: GetDownloadDirectoryUseCase
    private lateinit var downloadDirectoryRepository: DownloadDirectoryRepository
    private lateinit var useCase: GetCompletedDownloadFileUseCaseImpl

    private val directory = mockk<UniFile>()
    private val streamFile = mockk<UniFile>()
    private val streamUri = mockk<Uri>()

    private fun testItem(
        state: DownloadItemState = DownloadItemState.COMPLETED,
        streamUrl: String? = "https://example.com/stream.mp4",
    ) = DownloadItem(
        id = 1,
        mediaId = "media-1",
        mediaTitle = "Test Movie",
        mediaType = MediaType.MOVIE,
        state = state,
        streamUrl = streamUrl,
    )

    @Before
    fun setup() {
        getDownloadDirectoryUseCase = mockk()
        downloadDirectoryRepository = mockk()
        useCase = GetCompletedDownloadFileUseCaseImpl(
            getDownloadDirectoryUseCase = getDownloadDirectoryUseCase,
            downloadDirectoryRepository = downloadDirectoryRepository,
        )
    }

    @Test
    fun `invoke should return null when item is not completed`() =
        runTest {
            val result = useCase(testItem(state = DownloadItemState.STOPPED))

            expectThat(result).isNull()
        }

    @Test
    fun `invoke should return null when item has no stream url`() =
        runTest {
            val result = useCase(testItem(streamUrl = null))

            expectThat(result).isNull()
        }

    @Test
    fun `invoke should return null when download directory cannot be resolved`() =
        runTest {
            val item = testItem()
            coEvery {
                getDownloadDirectoryUseCase(item.mediaId, item.mediaTitle, item.seasonNumber, item.episodeNumber)
            } returns null

            val result = useCase(item)

            expectThat(result).isNull()
        }

    @Test
    fun `invoke should resolve the stream file uri and mime type from the download directory`() =
        runTest {
            val item = testItem()
            coEvery {
                getDownloadDirectoryUseCase(item.mediaId, item.mediaTitle, item.seasonNumber, item.episodeNumber)
            } returns directory
            every { downloadDirectoryRepository.getOrCreateFile(directory, "Test Movie.mp4") } returns streamFile
            every { streamFile.uri } returns streamUri
            every { streamFile.type } returns "video/mp4"

            val result = useCase(item)

            expectThat(result).isEqualTo(CompletedDownloadFile(uri = streamUri, mimeType = "video/mp4"))
        }

    @Test
    fun `invoke should fall back to a generic video mime type when the file type is blank`() =
        runTest {
            val item = testItem()
            coEvery {
                getDownloadDirectoryUseCase(item.mediaId, item.mediaTitle, item.seasonNumber, item.episodeNumber)
            } returns directory
            every { downloadDirectoryRepository.getOrCreateFile(directory, "Test Movie.mp4") } returns streamFile
            every { streamFile.uri } returns streamUri
            every { streamFile.type } returns null

            val result = useCase(item)

            expectThat(result).isEqualTo(CompletedDownloadFile(uri = streamUri, mimeType = "video/*"))
        }
}
