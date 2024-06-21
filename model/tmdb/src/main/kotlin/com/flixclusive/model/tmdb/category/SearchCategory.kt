package com.flixclusive.model.tmdb.category

import com.google.gson.annotations.SerializedName
import java.io.Serializable


/**
 * A model used for the search initial content screen.
 * It contains the query which is to be fetched thru
 * GET request in Retrofit2 instance of TMDBRepository.
 *
 * The hardcoded query will be useful for pagination.
 */
@kotlinx.serialization.Serializable
data class SearchCategory(
    @SerializedName("type") override val mediaType: String,
    @SerializedName("query") override val url: String,
    @SerializedName("poster_path") val posterPath: String? = null,
    override val name: String,
    override val canPaginate: Boolean = false,
    val id: Int,
) : Serializable, Category() {
    override fun equals(other: Any?): Boolean {
        val newData = other as? SearchCategory

        return name == newData?.name && url == newData.url
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + (posterPath?.hashCode() ?: 0)
        result = 31 * result + url.hashCode()
        result = 31 * result + mediaType.hashCode()
        return result
    }
}


/**
 * A model used for wrapping response of
 * Retrofit2 instance for [SearchCategory]
 *
 */
data class SearchCategoriesData(
    val networks: List<SearchCategory>,
    val companies: List<SearchCategory>,
    val genres: List<SearchCategory>,
    val type: List<SearchCategory>,
)