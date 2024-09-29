package com.flixclusive.data.configuration

data class AppUpdateInfo(
    val versionName: String,
    val updateUrl: String,
    val updateInfo: String? = null,
)