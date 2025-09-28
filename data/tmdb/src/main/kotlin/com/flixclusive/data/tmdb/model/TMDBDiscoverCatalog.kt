package com.flixclusive.data.tmdb.model

import com.flixclusive.model.provider.Catalog
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * A catalog model used for the discover screen.
 */
@kotlinx.serialization.Serializable
data class TMDBDiscoverCatalog(
    override val url: String,
    override val name: String,
    @SerializedName("poster_path") override val image: String? = null,
) : Catalog(),
    Serializable {
    override val canPaginate: Boolean = true

    override fun equals(other: Any?): Boolean {
        val newData = other as? TMDBDiscoverCatalog

        return name == newData?.name && url == newData.url
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    val isForTv: Boolean get() = url.contains("/tv?", true)
    val isForMovie: Boolean get() = url.contains("/movie?", true)
}

/**
 * A collection of [TMDBDiscoverCatalog]s for TMDB discovery operations.
 */
internal data class TMDBDiscoverCatalogs(
    val networks: List<TMDBDiscoverCatalog>,
    val companies: List<TMDBDiscoverCatalog>,
    val genres: List<TMDBDiscoverCatalog>,
    val type: List<TMDBDiscoverCatalog>,
)
