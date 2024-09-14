package com.flixclusive.model.provider

import com.flixclusive.model.provider.ProviderType.Companion.All
import com.flixclusive.model.provider.ProviderType.Companion.Movies
import com.flixclusive.model.provider.ProviderType.Companion.TvShows
import kotlinx.serialization.Serializable

/**
 * Represents the type of content the provider offers.
 *
 * @param type The provider type (e.g., "Movies", "TV Shows", or custom type).
 *
 * @see All
 * @see Movies
 * @see TvShows
 */
@Serializable
data class ProviderType(val type: String) {
    companion object {
        /** Quick instance of [ProviderType] for providers that provide all content. */
        val All = ProviderType("Movies, TV Shows, etc.")

        /** Quick instance of [ProviderType] for providers that provide movies. */
        val Movies = ProviderType("Movies")

        /** Quick instance of [ProviderType] for providers that provide tv shows. */
        val TvShows = ProviderType("TV Shows")
    }

    override fun equals(other: Any?): Boolean {
        return when(other) {
            is ProviderType -> other.type.equals(type, true)
            is String -> other.equals(type, true)
            else -> super.equals(other)
        }
    }

    override fun toString(): String {
        return type
    }

    override fun hashCode(): Int {
        return type.hashCode() * 31
    }
}
