package com.flixclusive.domain.model.config

import com.google.gson.annotations.SerializedName
import java.io.Serializable


/**
 * A model used for the homepage/dashboard.
 * It contains the query which is to be fetched thru
 * GET request in Retrofit2 instance of TMDBRepository.
 *
 * The hardcoded query will be useful for pagination.
 */
@kotlinx.serialization.Serializable
data class HomeCategoryItem(
    override val name: String,
    @SerializedName("type") override val mediaType: String,
    val required: Boolean,
    val canPaginate: Boolean,
    override val query: String,
) : Serializable, CategoryItem



/**
 * A model used for wrapping response of
 * Retrofit2 instance for [HomeCategoryItem]
 *
 */
data class HomeCategoriesConfig(
    val all: List<HomeCategoryItem>,
    val movie: List<HomeCategoryItem>,
    val tv: List<HomeCategoryItem>,
)