package com.flixclusive.model.datastore.provider

import kotlinx.serialization.Serializable


/**
 *
 * Data model to use to save provider
 * order and usability preferences.
 *
 * */
@Serializable
data class ProviderPreference(
    val name: String,
    val filePath: String,
    val isDisabled: Boolean,
    // TODO: Add lastUsedForSearching property
) {
    override fun equals(other: Any?): Boolean {
        return when(other) {
            is ProviderPreference -> name == other.name && filePath == other.filePath && isDisabled == other.isDisabled
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