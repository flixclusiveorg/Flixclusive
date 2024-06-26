package com.flixclusive.model.tmdb.category

import kotlinx.serialization.Serializable

const val DEFAULT_CATEGORY_MEDIA_TYPE = "all"

@Serializable
abstract class Category : java.io.Serializable {
    abstract val name: String
    abstract val url: String
    abstract val image: String?
    abstract val canPaginate: Boolean
    open val mediaType: String
        get() = DEFAULT_CATEGORY_MEDIA_TYPE
}