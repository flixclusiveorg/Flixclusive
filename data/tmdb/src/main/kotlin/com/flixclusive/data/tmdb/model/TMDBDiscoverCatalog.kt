package com.flixclusive.data.tmdb.model

import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.Catalog
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * A catalog model used for the discover screen.
 */
@kotlinx.serialization.Serializable
data class TMDBDiscoverCatalog(
    @SerializedName("type") override val mediaType: String,
    @SerializedName("query") override val url: String,
    @SerializedName("poster_path") override val image: String? = null,
    override val name: String,
    override val canPaginate: Boolean = false,
    val id: Int,
) : Catalog(),
    Serializable {
    override fun equals(other: Any?): Boolean {
        val newData = other as? TMDBDiscoverCatalog

        return name == newData?.name && url == newData.url
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + url.hashCode()
        result = 31 * result + mediaType.hashCode()
        return result
    }

    val isForTv: Boolean get() = mediaType == FilmType.TV_SHOW.type || mediaType == "all"
    val isForMovie: Boolean get() = mediaType == FilmType.MOVIE.type || mediaType == "all"
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
