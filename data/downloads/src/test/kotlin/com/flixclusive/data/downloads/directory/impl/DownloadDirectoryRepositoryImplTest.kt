package com.flixclusive.data.downloads.directory.impl

import com.hippo.unifile.UniFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class DownloadDirectoryRepositoryImplTest {
    private lateinit var repository: DownloadDirectoryRepositoryImpl

    private val mediaId = "123"
    private val mediaTitle = "Test Movie"

    @Before
    fun setup() {
        repository = DownloadDirectoryRepositoryImpl()
    }

    private fun mockDirectory(children: MutableMap<String, UniFile> = mutableMapOf()): UniFile {
        val dir = mockk<UniFile>()
        every { dir.findFile(any()) } answers { children[firstArg()] }
        every { dir.createDirectory(any()) } answers {
            val name = firstArg<String>()
            val created = mockDirectory()
            children[name] = created
            created
        }
        return dir
    }

    @Test
    fun `getOrCreateMediaDirectory should create Downloads and movie folders when absent`() {
        val root = mockDirectory()

        val result = repository.getOrCreateMediaDirectory(root, mediaId, mediaTitle)

        expectThat(result).isNotNull()
        verify { root.createDirectory("Downloads") }
    }

    @Test
    fun `getOrCreateMediaDirectory should reuse existing Downloads folder instead of recreating it`() {
        val existingDownloadsDir = mockDirectory()
        val root = mockDirectory(mutableMapOf("Downloads" to existingDownloadsDir))

        repository.getOrCreateMediaDirectory(root, mediaId, mediaTitle)

        verify(exactly = 0) { root.createDirectory("Downloads") }
        verify { existingDownloadsDir.createDirectory("123-Test Movie") }
    }

    @Test
    fun `getOrCreateMediaDirectory should return the media folder when season and episode are null`() {
        val downloadsDir = mockDirectory()
        val mediaDir = mockDirectory()
        every { downloadsDir.findFile("123-Test Movie") } returns null
        every { downloadsDir.createDirectory("123-Test Movie") } returns mediaDir
        val root = mockDirectory(mutableMapOf("Downloads" to downloadsDir))

        val result = repository.getOrCreateMediaDirectory(root, mediaId, mediaTitle)

        expectThat(result).isEqualTo(mediaDir)
    }

    @Test
    fun `getOrCreateMediaDirectory should also create the episode folder when season and episode are provided`() {
        val downloadsDir = mockDirectory()
        val mediaDir = mockDirectory()
        val episodeDir = mockDirectory()
        every { downloadsDir.createDirectory("123-Test Movie") } returns mediaDir
        every { mediaDir.createDirectory("s01e01") } returns episodeDir
        val root = mockDirectory(mutableMapOf("Downloads" to downloadsDir))

        val result = repository.getOrCreateMediaDirectory(
            root,
            mediaId,
            mediaTitle,
            seasonNumber = 1,
            episodeNumber = 1
        )

        expectThat(result).isEqualTo(episodeDir)
        verify { mediaDir.createDirectory("s01e01") }
    }

    @Test
    fun `getOrCreateMediaDirectory should return null when Downloads folder cannot be created`() {
        val root = mockk<UniFile>()
        every { root.findFile(any()) } returns null
        every { root.createDirectory(any()) } returns null

        val result = repository.getOrCreateMediaDirectory(root, mediaId, mediaTitle)

        expectThat(result).isNull()
    }

    @Test
    fun `getOrCreateSubtitlesDirectory should create subtitles folder under the media directory`() {
        val mediaDirectory = mockDirectory()

        repository.getOrCreateSubtitlesDirectory(mediaDirectory)

        verify { mediaDirectory.createDirectory("subtitles") }
    }

    @Test
    fun `getOrCreateFile should reuse an existing file instead of recreating it`() {
        val existingFile = mockk<UniFile>()
        val directory = mockk<UniFile>()
        every { directory.findFile("Pilot.mp4") } returns existingFile

        val result = repository.getOrCreateFile(directory, "Pilot.mp4")

        expectThat(result).isEqualTo(existingFile)
        verify(exactly = 0) { directory.createFile(any()) }
    }

    @Test
    fun `getOrCreateFile should create the file when it does not exist`() {
        val createdFile = mockk<UniFile>()
        val directory = mockk<UniFile>()
        every { directory.findFile("Pilot.mp4") } returns null
        every { directory.createFile("Pilot.mp4") } returns createdFile

        val result = repository.getOrCreateFile(directory, "Pilot.mp4")

        expectThat(result).isEqualTo(createdFile)
    }
}
