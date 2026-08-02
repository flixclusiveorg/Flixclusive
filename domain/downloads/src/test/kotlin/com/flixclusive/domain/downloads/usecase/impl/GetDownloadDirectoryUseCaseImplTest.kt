package com.flixclusive.domain.downloads.usecase.impl

import android.content.Context
import android.net.Uri
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.hippo.unifile.UniFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class GetDownloadDirectoryUseCaseImplTest {
    private lateinit var context: Context
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var downloadDirectoryRepository: DownloadDirectoryRepository
    private lateinit var useCase: GetDownloadDirectoryUseCaseImpl

    private val mediaId = "123"
    private val mediaTitle = "Test Movie"

    @Before
    fun setup() {
        context = mockk()
        dataStoreManager = mockk()
        downloadDirectoryRepository = mockk()

        useCase = GetDownloadDirectoryUseCaseImpl(
            context = context,
            dataStoreManager = dataStoreManager,
            downloadDirectoryRepository = downloadDirectoryRepository,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(UniFile::class)
        unmockkStatic(Uri::class)
    }

    private fun mockUriParsing() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
    }

    @Test
    fun `invoke should return null when storage directory uri is not set`() =
        runTest {
            every { dataStoreManager.getSystemPrefs() } returns flowOf(SystemPreferences(storageDirectoryUri = null))

            val result = useCase(mediaId, mediaTitle)

            expectThat(result).isNull()
        }

    @Test
    fun `invoke should return null when storage root cannot be accessed`() =
        runTest {
            every { dataStoreManager.getSystemPrefs() } returns
                flowOf(SystemPreferences(storageDirectoryUri = "content://test/tree"))
            mockUriParsing()

            mockkStatic(UniFile::class)
            every { UniFile.fromUri(context, any()) } returns null

            val result = useCase(mediaId, mediaTitle)

            expectThat(result).isNull()
        }

    @Test
    fun `invoke should return null when storage root is not writable`() =
        runTest {
            every { dataStoreManager.getSystemPrefs() } returns
                flowOf(SystemPreferences(storageDirectoryUri = "content://test/tree"))
            mockUriParsing()

            val root = mockk<UniFile>()
            every { root.isDirectory } returns true
            every { root.canWrite() } returns false

            mockkStatic(UniFile::class)
            every { UniFile.fromUri(context, any()) } returns root

            val result = useCase(mediaId, mediaTitle)

            expectThat(result).isNull()
        }

    @Test
    fun `invoke should delegate to repository when storage root is writable`() =
        runTest {
            every { dataStoreManager.getSystemPrefs() } returns
                flowOf(SystemPreferences(storageDirectoryUri = "content://test/tree"))
            mockUriParsing()

            val root = mockk<UniFile>()
            every { root.isDirectory } returns true
            every { root.canWrite() } returns true

            val mediaDir = mockk<UniFile>()
            every {
                downloadDirectoryRepository.getOrCreateMediaDirectory(root, mediaId, mediaTitle, null, null)
            } returns mediaDir

            mockkStatic(UniFile::class)
            every { UniFile.fromUri(context, any()) } returns root

            val result = useCase(mediaId, mediaTitle)

            expectThat(result).isEqualTo(mediaDir)
        }
}
