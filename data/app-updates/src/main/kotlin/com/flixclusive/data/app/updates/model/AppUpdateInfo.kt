package com.flixclusive.data.app.updates.model

/**
 * Represents information about an application update.
 *
 * @property versionName The version name of the update.
 * @property changelogs The changelogs or release notes for the update.
 * @property updateUrl The URL to download the update.
 * */
data class AppUpdateInfo(
    val versionName: String,
    val changelogs: String,
    val updateUrl: String,
)
