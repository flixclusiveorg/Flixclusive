package com.flixclusive.core.datastore.model.user

import kotlinx.serialization.Serializable

/**
 * User preferences for provider settings.
 *
 * Repository and provider data have been migrated to Room.
 * Only preference flags remain in DataStore.
 * */
@Serializable
data class ProviderPreferences(
    val shouldWarnBeforeInstall: Boolean = true,
    val isAutoUpdateEnabled: Boolean = true,
    val shouldAddDebugPrefix: Boolean = true,
) : UserPreferences
