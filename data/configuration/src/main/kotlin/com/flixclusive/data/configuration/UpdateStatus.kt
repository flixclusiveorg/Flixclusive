package com.flixclusive.data.configuration

import com.flixclusive.core.locale.UiText

sealed class UpdateStatus(
    val errorMessage: UiText? = null
) {
    data object Fetching : UpdateStatus()
    data class Outdated(val updateInfo: AppUpdateInfo) : UpdateStatus()
    data class UpToDate(val updateInfo: AppUpdateInfo) : UpdateStatus()
    class Error(errorMessage: UiText?) : UpdateStatus(errorMessage)
}