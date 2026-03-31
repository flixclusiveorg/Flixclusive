package com.flixclusive.core.datastore.model.user

import com.flixclusive.core.datastore.migration.model.OldProviderFromPreferences
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
    @Deprecated("This field is no longer used, as provider data has been migrated to Room. This field will be removed in a future version.")
    val providers: List<OldProviderFromPreferences> = emptyList(),
) : UserPreferences
