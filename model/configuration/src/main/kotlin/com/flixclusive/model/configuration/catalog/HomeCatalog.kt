package com.flixclusive.model.configuration.catalog

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
data class HomeCatalog(
    @SerializedName("query") override val url: String,
    @SerializedName("type") override val mediaType: String,
    override val name: String,
    override val canPaginate: Boolean,
    override val image: String? = null,
    val required: Boolean,
) : com.flixclusive.model.provider.Catalog()



/**
 * A model used for wrapping response of
 * Retrofit2 instance for [HomeCatalog]
 *
 */
data class HomeCatalogsData(
    val all: List<HomeCatalog>,
    val movie: List<HomeCatalog>,
    val tv: List<HomeCatalog>,
)