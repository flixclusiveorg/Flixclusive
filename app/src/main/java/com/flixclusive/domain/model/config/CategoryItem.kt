package com.flixclusive.domain.model.config

import kotlinx.serialization.Serializable

@Serializable
sealed interface CategoryItem {
    val name: String
    val query: String
    val mediaType: String
}