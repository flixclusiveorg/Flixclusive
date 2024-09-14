package com.flixclusive.model.provider

import kotlinx.serialization.Serializable

const val DEFAULT_CATALOG_MEDIA_TYPE = "all"

@Serializable
abstract class Catalog : java.io.Serializable {
    abstract val name: String
    abstract val url: String
    abstract val image: String?
    abstract val canPaginate: Boolean
    open val mediaType: String get() = DEFAULT_CATALOG_MEDIA_TYPE
}