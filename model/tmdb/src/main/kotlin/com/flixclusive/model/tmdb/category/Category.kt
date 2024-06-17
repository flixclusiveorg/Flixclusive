package com.flixclusive.model.tmdb.category

import kotlinx.serialization.Serializable

const val DEFAULT_CATEGORY_MEDIA_TYPE = "all"

@Serializable
abstract class Category : java.io.Serializable {
    abstract val name: String
    abstract val url: String
    open val mediaType: String
        get() = DEFAULT_CATEGORY_MEDIA_TYPE
}