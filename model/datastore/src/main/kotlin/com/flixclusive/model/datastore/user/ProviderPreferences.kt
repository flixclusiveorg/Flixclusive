package com.flixclusive.model.datastore.user

import com.flixclusive.model.provider.Repository
import kotlinx.serialization.Serializable

/**
 *
 * A sub data class for provider settings of
 * the main [AppSettings] data class.
 * */
@Serializable
data class ProviderPreferences(
    val shouldWarnBeforeInstall: Boolean = true,
    val isAutoUpdateEnabled: Boolean = true,
    val repositories: List<Repository> = listOf(),
    val providers: List<ProviderFromPreferences> = emptyList(),
) : UserPreferences

/**
 *
 * Data model to use to save provider
 * order and usability preferences.
 *
 * */
@Serializable
data class ProviderFromPreferences(
    val id: String = "",
    val name: String,
    val filePath: String,
    val isDisabled: Boolean,
    val isDebug: Boolean = false,
) {
    override fun equals(other: Any?): Boolean =
        when (other) {
            is ProviderFromPreferences -> id == other.id
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
