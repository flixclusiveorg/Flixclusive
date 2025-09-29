package com.flixclusive.data.app.updates.repository.impl

import com.flixclusive.core.common.config.AppVersion
import com.flixclusive.core.common.config.BuildConfigProvider
import com.flixclusive.core.common.config.BuildType
import com.flixclusive.core.common.config.CustomBuildConfig
import com.flixclusive.core.common.config.PlatformType
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.network.retrofit.dto.GithubRelease
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.util.log.LogRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isTrue

class GithubUpdatesRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var githubApiService: GithubApiService
    private lateinit var githubUpdatesRepository: GithubUpdatesRepository
    private lateinit var buildConfigProvider: BuildConfigProvider
    private lateinit var appDispatchers: AppDispatchers

    private val mockGithubRelease = mockk<GithubRelease>()
    private val mockBuildConfig = mockk<CustomBuildConfig>()

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setup() {
        githubApiService = mockk()
        buildConfigProvider = mockk()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        every { buildConfigProvider.get() } returns mockBuildConfig

        githubUpdatesRepository = GithubUpdatesRepository(
            apiService = githubApiService,
            buildConfigProvider = buildConfigProvider,
            appDispatchers = appDispatchers,
        )
    }

    @Test
    fun `when stable release has newer version then returns update info`() =
        runTest(testDispatcher) {
            val currentVersion = AppVersion.from(BuildType.STABLE, "1.0.0")
            val newerVersion = "1.1.0"
            val expectedUpdateUrl = """
                https://github.com/flixclusiveorg/Flixclusive/releases/download/v1.1.0/app-mobile.apk
            """.trimIndent()
            val expectedChangelogs = "Bug fixes and improvements"

            every { mockBuildConfig.buildType } returns BuildType.STABLE
            every { mockBuildConfig.version } returns currentVersion
            every { mockBuildConfig.platformType } returns PlatformType.MOBILE
            every { mockGithubRelease.tagName } returns newerVersion
            every { mockGithubRelease.releaseNotes } returns expectedChangelogs
            every { mockGithubRelease.getDownloadUrl(PlatformType.MOBILE) } returns expectedUpdateUrl

            coEvery {
                githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive")
            } returns mockGithubRelease

            val result = githubUpdatesRepository.getLatestUpdate()

            expectThat(result.isSuccess).isTrue()
            result.getOrNull()?.let { updateInfo ->
                expectThat(updateInfo.versionName).isEqualTo("1.1.0")
                expectThat(updateInfo.changelogs).isEqualTo(expectedChangelogs)
                expectThat(updateInfo.updateUrl).isEqualTo(expectedUpdateUrl)
            }
            coVerify { githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive") }
        }

    @Test
    fun `when preview release has newer version then returns update info`() =
        runTest(testDispatcher) {
            val currentVersion = AppVersion.from(BuildType.PREVIEW, "100")
            val newerVersionTag = "150"
            val expectedUpdateUrl = """
                https://github.com/flixclusiveorg/preview-builds/releases/download/p150/app-mobile.apk
            """.trimIndent()
            val expectedChangelogs = "Preview build with new features"

            every { mockBuildConfig.buildType } returns BuildType.PREVIEW
            every { mockBuildConfig.version } returns currentVersion
            every { mockBuildConfig.platformType } returns PlatformType.MOBILE
            every { mockGithubRelease.tagName } returns newerVersionTag
            every { mockGithubRelease.releaseNotes } returns expectedChangelogs
            every { mockGithubRelease.getDownloadUrl(PlatformType.MOBILE) } returns expectedUpdateUrl

            coEvery {
                githubApiService.getLatestRelease("flixclusiveorg", "preview-builds")
            } returns mockGithubRelease

            val result = githubUpdatesRepository.getLatestUpdate()

            expectThat(result.isSuccess).isTrue()
            result.getOrNull()?.let { updateInfo ->
                expectThat(updateInfo.versionName).isEqualTo("p150")
                expectThat(updateInfo.changelogs).isEqualTo(expectedChangelogs)
                expectThat(updateInfo.updateUrl).isEqualTo(expectedUpdateUrl)
            }
            coVerify { githubApiService.getLatestRelease("flixclusiveorg", "preview-builds") }
        }

    @Test
    fun `when current version is same as latest then returns null`() =
        runTest(testDispatcher) {
            val currentVersion = AppVersion.from(BuildType.STABLE, "1.0.0")
            val sameVersion = "1.0.0"

            every { mockBuildConfig.buildType } returns BuildType.STABLE
            every { mockBuildConfig.version } returns currentVersion
            every { mockBuildConfig.platformType } returns PlatformType.MOBILE
            every { mockGithubRelease.tagName } returns sameVersion

            coEvery {
                githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive")
            } returns mockGithubRelease

            val result = githubUpdatesRepository.getLatestUpdate()

            expectThat(result.isSuccess).isTrue()
            expectThat(result.getOrNull()).isNull()
            coVerify { githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive") }
        }

    @Test
    fun `when current version is newer than latest then returns null`() =
        runTest(testDispatcher) {
            val currentVersion = AppVersion.from(BuildType.STABLE, "1.2.0")
            val olderVersion = "1.0.0"

            every { mockBuildConfig.buildType } returns BuildType.STABLE
            every { mockBuildConfig.version } returns currentVersion
            every { mockBuildConfig.platformType } returns PlatformType.MOBILE
            every { mockGithubRelease.tagName } returns olderVersion

            coEvery {
                githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive")
            } returns mockGithubRelease

            val result = githubUpdatesRepository.getLatestUpdate()

            expectThat(result.isSuccess).isTrue()
            expectThat(result.getOrNull()).isNull()
            coVerify { githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive") }
        }

    @Test
    fun `when tv platform and newer version available then returns update info for tv`() =
        runTest(testDispatcher) {
            val currentVersion = AppVersion.from(BuildType.STABLE, "1.0.0")
            val newerVersion = "1.1.0"
            val expectedUpdateUrl = "https://github.com/flixclusiveorg/Flixclusive/releases/download/v1.1.0/app-tv.apk"
            val expectedChangelogs = "TV version improvements"

            every { mockBuildConfig.buildType } returns BuildType.STABLE
            every { mockBuildConfig.version } returns currentVersion
            every { mockBuildConfig.platformType } returns PlatformType.TV
            every { mockGithubRelease.tagName } returns newerVersion
            every { mockGithubRelease.releaseNotes } returns expectedChangelogs
            every { mockGithubRelease.getDownloadUrl(PlatformType.TV) } returns expectedUpdateUrl

            coEvery {
                githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive")
            } returns mockGithubRelease

            val result = githubUpdatesRepository.getLatestUpdate()

            expectThat(result.isSuccess).isTrue()
            result.getOrNull()?.let { updateInfo ->
                expectThat(updateInfo.versionName).isEqualTo("1.1.0")
                expectThat(updateInfo.changelogs).isEqualTo(expectedChangelogs)
                expectThat(updateInfo.updateUrl).isEqualTo(expectedUpdateUrl)
            }
            coVerify { githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive") }
        }

    @Test
    fun `when download url is null then returns failure with ui error`() =
        runTest(testDispatcher) {
            val currentVersion = AppVersion.from(BuildType.STABLE, "1.0.0")
            val newerVersion = "1.1.0"

            every { mockBuildConfig.buildType } returns BuildType.STABLE
            every { mockBuildConfig.version } returns currentVersion
            every { mockBuildConfig.platformType } returns PlatformType.MOBILE
            every { mockGithubRelease.tagName } returns newerVersion
            every { mockGithubRelease.getDownloadUrl(PlatformType.MOBILE) } returns null

            coEvery {
                githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive")
            } returns mockGithubRelease

            val result = githubUpdatesRepository.getLatestUpdate()

            expectThat(result.isFailure).isTrue()
            expectThat(result.exceptionOrNull()).isA<ExceptionWithUiText>()
            coVerify { githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive") }
        }

    @Test
    fun `when api call throws exception then returns failure with ui error`() =
        runTest(testDispatcher) {
            val networkException = Exception("Network error")

            every { mockBuildConfig.buildType } returns BuildType.STABLE

            coEvery {
                githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive")
            } throws networkException

            val result = githubUpdatesRepository.getLatestUpdate()

            expectThat(result.isFailure).isTrue()
            expectThat(result.exceptionOrNull()).isA<ExceptionWithUiText>()
            coVerify { githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive") }
        }

    @Test
    fun `when api call throws ui exception then returns that exception`() =
        runTest(testDispatcher) {
            val uiException = mockk<ExceptionWithUiText>()

            every { mockBuildConfig.buildType } returns BuildType.STABLE

            coEvery {
                githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive")
            } throws uiException

            val result = githubUpdatesRepository.getLatestUpdate()

            expectThat(result.isFailure).isTrue()
            expectThat(result.exceptionOrNull()).isEqualTo(uiException)
            coVerify { githubApiService.getLatestRelease("flixclusiveorg", "Flixclusive") }
        }
}
