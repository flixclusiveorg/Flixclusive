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
    val warnOnInstall: Boolean = true,
    val autoUpdate: Boolean = true,
    val repositories: List<Repository> = listOf(),
    val providers: List<ProviderOrderEntity> = emptyList(),
) : UserPreferences


/**
 *
 * Data model to use to save provider
 * order and usability preferences.
 *
 * */
@Serializable
data class ProviderOrderEntity(
    val name: String,
    val filePath: String,
    val isDisabled: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        return when(other) {
            is ProviderOrderEntity -> name == other.name && filePath == other.filePath && isDisabled == other.isDisabled
            is String -> filePath == other
            else -> super.equals(other)
        }
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + isDisabled.hashCode()
        return result
    }
}