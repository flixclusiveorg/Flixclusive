package com.flixclusive.data.app.updates.repository.impl

import com.flixclusive.core.common.config.AppVersion
import com.flixclusive.core.common.config.BuildConfigProvider
import com.flixclusive.core.common.config.BuildType
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.app.updates.R
import com.flixclusive.data.app.updates.model.AppUpdateInfo
import com.flixclusive.data.app.updates.repository.AppUpdatesRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class GithubUpdatesRepository
    @Inject
    constructor(
        private val apiService: GithubApiService,
        private val buildConfigProvider: BuildConfigProvider,
        private val appDispatchers: AppDispatchers,
    ) : AppUpdatesRepository {
        companion object {
            private const val GITHUB_ORG = "flixclusiveorg"

            private const val STABLE_CHANNEL_NAME = "Flixclusive"
            private const val PREVIEW_CHANNEL_NAME = "preview-builds"
        }

        override suspend fun getLatestUpdate(): Result<AppUpdateInfo?> {
            val buildConfig = buildConfigProvider.get()

            val repository = when (buildConfig.buildType) {
                BuildType.PREVIEW, BuildType.DEBUG -> PREVIEW_CHANNEL_NAME
                else -> STABLE_CHANNEL_NAME
            }

            return withContext(appDispatchers.io) {
                try {
                    val releases = apiService.getLatestRelease(
                        owner = GITHUB_ORG,
                        repository = repository,
                    )

                    val currentVersion = buildConfig.version
                    val latestVersion = AppVersion.from(
                        buildType = buildConfig.buildType,
                        version = releases.tagName,
                    )

                    // Current version is newer or the same as the latest version
                    if (currentVersion >= latestVersion) {
                        return@withContext Result.success(null)
                    }

                    val updateUrl = releases.getDownloadUrl(buildConfig.platformType)
                        ?: throw ExceptionWithUiText(
                            uiText = UiText.from(R.string.failed_to_fetch_apk_url, buildConfig.platformType.toString()),
                        )

                    Result.success(
                        AppUpdateInfo(
                            versionName = latestVersion.toString(),
                            changelogs = releases.releaseNotes,
                            updateUrl = updateUrl,
                        ),
                    )
                } catch (e: ExceptionWithUiText) {
                    Result.failure(e)
                } catch (e: Exception) {
                    errorLog("!! Failed to check for updates !!")
                    errorLog(e)

                    Result.failure(
                        ExceptionWithUiText(
                            UiText.from(
                                R.string.failed_to_check_for_updates,
                                e.localizedMessage ?: "Unknown error",
                            ),
                        ),
                    )
                }
            }
        }
    }
