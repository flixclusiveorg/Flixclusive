package com.flixclusive.model.configuration

import kotlinx.serialization.Serializable

@Serializable
sealed interface CategoryItem {
    val name: String
    val query: String
    val mediaType: String
}