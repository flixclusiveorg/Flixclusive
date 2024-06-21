package com.flixclusive.model.provider

import com.flixclusive.model.tmdb.category.Category
import kotlinx.serialization.Serializable

@Serializable
data class ProviderCatalog(
    override val name: String,
    override val url: String,
    override val canPaginate: Boolean,
    val providerName: String,
) : Category()