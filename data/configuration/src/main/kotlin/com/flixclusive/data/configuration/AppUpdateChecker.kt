package com.flixclusive.data.configuration

import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.util.common.GithubConstant
import retrofit2.HttpException
import java.time.Instant
import javax.inject.Inject

class AppUpdateChecker
    @Inject
    constructor(
        private val githubApiService: GithubApiService,
    ) {
        suspend fun checkForPrereleaseUpdates(currentAppBuild: AppBuild): UpdateStatus {
            return safeNetworkCall(currentAppBuild) { currentAppUpdateInfo ->
                val releases = githubApiService.getReleases()
                val preReleases = releases.filter { it.isPrerelease }

                val appCommitVersion = currentAppBuild.commitVersion
                var currentTag = preReleases.find { it.tagName.contains(appCommitVersion, true) }

                if (currentTag == null) {
                    val versionName = currentAppBuild.versionName
                    currentTag = releases.find { it.tagName == versionName }
                        ?: return@safeNetworkCall UpdateStatus.UpToDate(updateInfo = currentAppUpdateInfo)
                }

                val latestPreRelease = preReleases.first()
                val latestPreReleaseDateMs = Instant.parse(latestPreRelease.createdAt).toEpochMilli()
                val currentTagDateMs = Instant.parse(currentTag.createdAt).toEpochMilli()

                val isNeedingAnUpdate = latestPreReleaseDateMs > currentTagDateMs

                if (isNeedingAnUpdate) {
                    val newAppConfig =
                        AppUpdateInfo(
                            versionName = "${latestPreRelease.name} \uD83D\uDDFF",
                            updateInfo = latestPreRelease.releaseNotes,
                            updateUrl = getFlixclusiveApkDownloadUrl(latestPreRelease.tagName),
                        )

                    return UpdateStatus.Outdated(updateInfo = newAppConfig)
                }

                return UpdateStatus.UpToDate(
                    updateInfo =
                        AppUpdateInfo(
                            versionName = currentTag.name,
                            updateInfo = currentTag.releaseNotes,
                            updateUrl = getFlixclusiveApkDownloadUrl(latestPreRelease.tagName),
                        ),
                )
            }
        }

        suspend fun checkForStableUpdates(currentAppBuild: AppBuild): UpdateStatus {
            return safeNetworkCall(currentAppBuild) { currentAppUpdateInfo ->
                val latestStableRelease = githubApiService.getStableReleaseInfo()
                val currentReleaseInfo =
                    githubApiService.getReleaseInfo(tag = currentAppBuild.versionName)

                val latestReleaseCreationDate =
                    Instant.parse(latestStableRelease.createdAt).toEpochMilli()
                val currentReleaseCreationDate =
                    Instant.parse(currentReleaseInfo.createdAt).toEpochMilli()

                val latestSemVer = parseSemVer(latestStableRelease.name)
                val currentSemVer = parseSemVer(currentAppBuild.versionName)

                val isNeedingAnUpdate =
                    latestReleaseCreationDate > currentReleaseCreationDate &&
                        latestSemVer > currentSemVer

                if (isNeedingAnUpdate) {
                    val newAppUpdateInfo =
                        AppUpdateInfo(
                            versionName = latestStableRelease.name,
                            updateInfo = latestStableRelease.releaseNotes,
                            updateUrl = getFlixclusiveApkDownloadUrl(latestStableRelease.tagName),
                        )

                    return UpdateStatus.Outdated(updateInfo = newAppUpdateInfo)
                }

                return UpdateStatus.UpToDate(updateInfo = currentAppUpdateInfo)
            }
        }

        private inline fun safeNetworkCall(
            currentAppBuild: AppBuild,
            block: (AppUpdateInfo) -> UpdateStatus,
        ): UpdateStatus {
            val currentAppUpdateInfo =
                AppUpdateInfo(
                    versionName = currentAppBuild.versionName,
                    updateUrl = getFlixclusiveApkDownloadUrl(currentAppBuild.versionName),
                )

            try {
                return block.invoke(currentAppUpdateInfo)
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                if (e.code() == 404 || body?.contains("Not Found") == true) {
                    return UpdateStatus.UpToDate(updateInfo = currentAppUpdateInfo)
                }

                throw e
            }
        }

        private fun parseSemVer(version: String): SemanticVersion {
            val regex = Regex("""(\d+)\.(\d+)\.(\d+)""")
            val match = regex.find(version)

            return match?.let {
                val (major, minor, patch) = it.destructured
                SemanticVersion(major.toInt(), minor.toInt(), patch.toInt())
            } ?: SemanticVersion(
                major = -1,
                minor = -1,
                patch = -1,
            )
        }

        private data class SemanticVersion(
            val major: Int,
            val minor: Int,
            val patch: Int,
        ) : Comparable<SemanticVersion> {
            override fun compareTo(other: SemanticVersion): Int {
                // Compare major versions
                if (this.major != other.major) {
                    return this.major.compareTo(other.major)
                }

                // Compare minor versions
                if (this.minor != other.minor) {
                    return this.minor.compareTo(other.minor)
                }

                // Compare patch versions
                return this.patch.compareTo(other.patch)
            }
        }

        private fun getFlixclusiveApkDownloadUrl(tag: String): String {
            return "https://github.com/${GithubConstant.GITHUB_USERNAME}/${GithubConstant.GITHUB_REPOSITORY}/releases/download/$tag/flixclusive-release.apk"
        }
    }
