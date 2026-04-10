package com.flixclusive.core.datastore.model.user

import kotlinx.serialization.Serializable

@Serializable
data class BackupOptions(
    val includeLibrary: Boolean = true,
    val includeWatchProgress: Boolean = true,
    val includeSearchHistory: Boolean = true,
    val includePreferences: Boolean = true,
    val includeProviders: Boolean = true,
    val includeRepositories: Boolean = true,
)
