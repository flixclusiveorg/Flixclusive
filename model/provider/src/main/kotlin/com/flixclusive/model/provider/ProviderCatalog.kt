package com.flixclusive.model.provider

import kotlinx.serialization.Serializable

@Serializable
data class ProviderCatalog(
    override val name: String,
    override val url: String,
    override val canPaginate: Boolean,
    override val image: String? = null,
    val providerName: String,
) : Catalog()