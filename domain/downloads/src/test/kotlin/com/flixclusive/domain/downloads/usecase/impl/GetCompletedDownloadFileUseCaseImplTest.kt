package com.flixclusive.domain.downloads.usecase.impl

import android.net.Uri
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.domain.downloads.usecase.CompletedSubtitleFile
import com.flixclusive.model.media.common.MediaType
import com.hippo.unifile.UniFile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class GetCompletedDownloadFileUseCaseImplTest {
    private lateinit var downloadDirectoryRepository: DownloadDirectoryRepository
    private lateinit var useCase: GetCompletedDownloadFileUseCaseImpl

    private val streamFile = mockk<UniFile>()
    private val streamUri = mockk<Uri>()

    private fun testItem(
        state: DownloadItemState = DownloadItemState.COMPLETED,
        streamFilePath: String? = "content://tree/video.mp4",
    ) = DownloadItem(
        id = "item-1",
        ownerId = "owner-1",
        mediaId = "media-1",
        mediaTitle = "Test Movie",
        mediaType = MediaType.MOVIE,
        state = state,
        streamFilePath = streamFilePath,
    )

    @Before
    fun setup() {
        downloadDirectoryRepository = mockk()
        useCase = GetCompletedDownloadFileUseCaseImpl(
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
    fun `invoke should return null when item has no stream file path`() =
        runTest {
            val result = useCase(testItem(streamFilePath = null))

            expectThat(result).isNull()
        }

    @Test
    fun `invoke should return null instead of a fresh placeholder when the stream file path no longer resolves`() =
        runTest {
            val item = testItem()
            every { downloadDirectoryRepository.resolveFile(item.streamFilePath!!) } returns null

            val result = useCase(item)

            expectThat(result).isNull()
        }

    @Test
    fun `invoke should resolve the stream file uri and mime type from its persisted path`() =
        runTest {
            val item = testItem()
            every { downloadDirectoryRepository.resolveFile(item.streamFilePath!!) } returns streamFile
            every { downloadDirectoryRepository.listSubtitleFiles(streamFile) } returns emptyList()
            every { streamFile.uri } returns streamUri
            every { streamFile.type } returns "video/mp4"

            val result = useCase(item)

            expectThat(result).isEqualTo(CompletedDownloadFile(uri = streamUri, mimeType = "video/mp4"))
        }

    @Test
    fun `invoke should fall back to a generic video mime type when the file type is blank`() =
        runTest {
            val item = testItem()
            every { downloadDirectoryRepository.resolveFile(item.streamFilePath!!) } returns streamFile
            every { downloadDirectoryRepository.listSubtitleFiles(streamFile) } returns emptyList()
            every { streamFile.uri } returns streamUri
            every { streamFile.type } returns null

            val result = useCase(item)

            expectThat(result).isEqualTo(CompletedDownloadFile(uri = streamUri, mimeType = "video/*"))
        }

    @Test
    fun `invoke should surface sibling subtitle files with their language parsed from the file name`() =
        runTest {
            val item = testItem()
            val subtitleFile = mockk<UniFile>()
            val subtitleUri = mockk<Uri>()
            every { downloadDirectoryRepository.resolveFile(item.streamFilePath!!) } returns streamFile
            every { downloadDirectoryRepository.listSubtitleFiles(streamFile) } returns listOf(subtitleFile)
            every { streamFile.uri } returns streamUri
            every { streamFile.type } returns "video/mp4"
            every { subtitleFile.uri } returns subtitleUri
            every { subtitleFile.name } returns "Test Movie (English).srt"

            val result = useCase(item)

            expectThat(result?.subtitles).isEqualTo(
                listOf(CompletedSubtitleFile(uri = subtitleUri, language = "English"))
            )
        }
}
