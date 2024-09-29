package com.flixclusive.domain.updater

import com.flixclusive.data.configuration.AppConfigurationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateCheckerUseCase @Inject constructor(
    private val appConfigurationManager: AppConfigurationManager,
) {
    val newVersion: String?
        get() = appConfigurationManager.appUpdateInfo?.versionName
    val updateInfo: String?
        get() = appConfigurationManager.appUpdateInfo?.updateInfo
    val updateUrl: String?
        get() = appConfigurationManager.appUpdateInfo?.updateUrl

    val updateStatus = appConfigurationManager.updateStatus

    suspend fun checkForUpdates() {
        appConfigurationManager.checkForUpdates()
    }
}