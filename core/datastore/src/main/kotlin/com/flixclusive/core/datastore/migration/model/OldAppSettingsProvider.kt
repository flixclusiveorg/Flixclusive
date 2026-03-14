package com.flixclusive.core.datastore.migration.model

import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.model.provider.Repository
import kotlinx.serialization.Serializable


@Deprecated("Provider preferences have been migrated to Room. Only preference flags remain in DataStore.")
@Serializable
internal data class OldAppSettingsProvider(
    val warnOnInstall: Boolean = true,
    val isUsingAutoUpdateProviderFeature: Boolean = true,
    val repositories: List<Repository> = listOf(),
    val providers: List<OldProviderFromPreferences> = emptyList(),
)

@Deprecated("Provider preferences have been migrated to Room. Only preference flags remain in DataStore.")
@Serializable
internal data class ProviderPreferencesV213(
    val shouldWarnBeforeInstall: Boolean = true,
    val isAutoUpdateEnabled: Boolean = true,
    val shouldAddDebugPrefix: Boolean = true,
    val repositories: List<Repository> = listOf(),
    val providers: List<OldProviderFromPreferences> = emptyList(),
) : UserPreferences


@Deprecated("Provider preferences have been migrated to Room. Only preference flags remain in DataStore.")
@Serializable
internal data class OldProviderFromPreferences(
    val id: String = "",
    val name: String,
    val filePath: String,
    val isDisabled: Boolean,
    val isDebug: Boolean = false,
) {
    override fun equals(other: Any?): Boolean =
        when (other) {
            is OldProviderFromPreferences -> id == other.id
            is String -> id == other
            else -> super.equals(other)
        }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + isDisabled.hashCode()
        result = 31 * result + isDebug.hashCode()
        return result
    }
}

