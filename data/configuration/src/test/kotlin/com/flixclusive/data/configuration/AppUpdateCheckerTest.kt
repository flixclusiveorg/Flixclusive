package com.flixclusive.data.configuration

import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.util.common.GithubConstant
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppUpdateCheckerTest {

    private lateinit var githubApiService: GithubApiService

    private lateinit var appUpdateChecker: AppUpdateChecker

    private val baseAppBuild = AppBuild(
        build = 10000,
        versionName = "2.1.2",
        commitVersion = "abc123",
        debug = false,
        applicationId = "",
        applicationName = ""
    )

    @Before
    fun setup() {
        githubApiService = Retrofit.Builder()
            .baseUrl(GithubConstant.GITHUB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient())
            .build()
            .create(GithubApiService::class.java)

        appUpdateChecker = AppUpdateChecker(githubApiService)
    }

    @Test
    fun `checkForPrereleaseUpdates should return Outdated when update is available`() = runTest {
        // Arrange
        val preReleases = githubApiService.getReleases().filter { it.isPrerelease }
        val latestPreRelease = preReleases.last()
        val latestCommitHash = latestPreRelease.tagName.removePrefix("PR-")
        val currentAppBuild = baseAppBuild.copy(commitVersion = latestCommitHash)

        // Act
        val result = appUpdateChecker.checkForPrereleaseUpdates(
            currentAppBuild = currentAppBuild
        )

        // Assert
        assertTrue(result is UpdateStatus.Outdated)
    }

    @Test
    fun `checkForPrereleaseUpdates should return UpToDate when no update is available`() = runTest {
        // Arrange
        val preReleases = githubApiService.getReleases().filter { it.isPrerelease }
        val latestPreRelease = preReleases.first()
        val latestCommitHash = latestPreRelease.tagName.removePrefix("PR-")
        val currentAppBuild = baseAppBuild.copy(commitVersion = latestCommitHash)

        // Act
        val result = appUpdateChecker.checkForPrereleaseUpdates(
            currentAppBuild = currentAppBuild
        )

        // Assert
        assertTrue(result is UpdateStatus.UpToDate)
    }

    @Test
    fun `checkForStableUpdates should return Outdated when update is available`() = runTest {
        // Act
        val result = appUpdateChecker.checkForStableUpdates(baseAppBuild)

        // Assert
        assertTrue(result is UpdateStatus.Outdated)
    }

    @Test
    fun `checkForStableUpdates should return UpToDate when no update is available`() = runTest {
        // Arrange
        val currentAppBuild = baseAppBuild.copy(versionName = "2.1.3")

        // Act
        val result = appUpdateChecker.checkForStableUpdates(currentAppBuild)

        // Assert
        assertTrue(result is UpdateStatus.UpToDate)
    }
}
