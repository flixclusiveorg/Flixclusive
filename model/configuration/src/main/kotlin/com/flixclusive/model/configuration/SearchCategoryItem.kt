package com.flixclusive.model.configuration

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
data class SearchCategoryItem(
    val id: Int,
    override val name: String,
    @SerializedName("poster_path") val posterPath: String? = null,
    override val query: String,
    @SerializedName("type") override val mediaType: String
) : Serializable, CategoryItem {
    override fun equals(other: Any?): Boolean {
        val newData = other as SearchCategoryItem

        return name == newData.name && query == newData.query
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + (posterPath?.hashCode() ?: 0)
        result = 31 * result + query.hashCode()
        result = 31 * result + mediaType.hashCode()
        return result
    }
}


/**
 * A model used for wrapping response of
 * Retrofit2 instance for [SearchCategoryItem]
 *
 */
data class SearchCategoriesConfig(
    val networks: List<SearchCategoryItem>,
    val companies: List<SearchCategoryItem>,
    val genres: List<SearchCategoryItem>,
    val type: List<SearchCategoryItem>,
)