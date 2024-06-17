package com.flixclusive.model.tmdb.category

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable


/**
 * A model used for the homepage/dashboard.
 * It contains the query which is to be fetched thru
 * GET request in Retrofit2 instance of TMDBRepository.
 *
 * The hardcoded query will be useful for pagination.
 */
@Serializable
data class HomeCategory(
    @SerializedName("query") override val url: String,
    @SerializedName("type") override val mediaType: String,
    override val name: String,
    val canPaginate: Boolean,
    val required: Boolean,
) : Category()



/**
 * A model used for wrapping response of
 * Retrofit2 instance for [HomeCategory]
 *
 */
data class HomeCategoriesData(
    val all: List<HomeCategory>,
    val movie: List<HomeCategory>,
    val tv: List<HomeCategory>,
)