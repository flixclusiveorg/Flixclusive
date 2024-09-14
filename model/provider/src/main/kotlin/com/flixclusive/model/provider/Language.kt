package com.flixclusive.model.provider

import com.flixclusive.model.provider.Language.Companion.Multiple
import kotlinx.serialization.Serializable

/**
 * Represents the language of a provider.
 *
 * @param languageCode The shorthand code representing the language (e.g., "en", "fr", "ph") or "Multiple" for providers with multiple languages.
 *
 * @see Multiple
 */
@Serializable
data class Language(val languageCode: String) {
    companion object {
        /** Quick instance of [Language] for providers with multiple languages. */
        val Multiple = Language("Multiple")
    }

    override fun toString(): String {
        return languageCode
    }
}
