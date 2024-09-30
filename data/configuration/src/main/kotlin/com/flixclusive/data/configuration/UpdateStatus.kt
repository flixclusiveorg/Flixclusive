package com.flixclusive.data.configuration

import com.flixclusive.core.locale.UiText

sealed class UpdateStatus(
    val errorMessage: UiText? = null,
    val updateInfo: AppUpdateInfo? = null,
) {
    data object Fetching : UpdateStatus()
    class Outdated(updateInfo: AppUpdateInfo) : UpdateStatus(updateInfo = updateInfo)
    class UpToDate(updateInfo: AppUpdateInfo) : UpdateStatus(updateInfo = updateInfo)
    class Error(errorMessage: UiText?) : UpdateStatus(errorMessage)
}