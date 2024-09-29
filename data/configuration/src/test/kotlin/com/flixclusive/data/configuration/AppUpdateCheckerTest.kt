package com.flixclusive.data.configuration

import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.util.common.GithubConstant
import kotlinx.coroutines.runBlocking
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
        versionName = "v1.4.0-beta1",
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
    fun `checkForPrereleaseUpdates should return Outdated when update is available`() = runBlocking {
        // Act
        val result = appUpdateChecker.checkForPrereleaseUpdates(
            currentAppBuild = baseAppBuild
        )

        // Assert
        assertTrue(result is UpdateStatus.Outdated)
    }

    @Test
    fun `checkForPrereleaseUpdates should return UpToDate when no update is available`() = runBlocking {
        // Arrange
        val currentAppBuild = baseAppBuild.copy(commitVersion = "5fe0a77")

        // Act
        val result = appUpdateChecker.checkForPrereleaseUpdates(
            currentAppBuild = currentAppBuild
        )

        // Assert
        assertTrue(result is UpdateStatus.UpToDate)
    }

    @Test
    fun `checkForStableUpdates should return Outdated when update is available`() = runBlocking {
        // Act
        val result = appUpdateChecker.checkForStableUpdates(baseAppBuild)

        // Assert
        assertTrue(result is UpdateStatus.Outdated)
    }

    @Test
    fun `checkForStableUpdates should return UpToDate when no update is available`() = runBlocking {
        // Arrange
        val currentAppBuild = baseAppBuild.copy(versionName = "2.0.1")

        // Act
        val result = appUpdateChecker.checkForStableUpdates(currentAppBuild)

        // Assert
        assertTrue(result is UpdateStatus.UpToDate)
    }
}