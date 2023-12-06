package com.flixclusive.providers.models.providers.superstream

import com.flixclusive.providers.models.common.SearchResultItem
import com.flixclusive.providers.sources.superstream.utils.SuperStreamUtils
import com.google.gson.annotations.SerializedName

internal data class SuperStreamSearchResponse(
    val code: String? = null,
    val msg: String? = null,
    val data: SuperStreamSearchResponseDataContent = SuperStreamSearchResponseDataContent()
) {
    data class SuperStreamSearchResponseDataContent(
        @SerializedName("list") val results: List<SuperStreamSearchItem> = listOf(),
        val total: Int = 0,
    )
    data class SuperStreamSearchItem(
        val id: Int? = null,
        @SerializedName("box_type") val boxType: Int? = null,
        val title: String? = null,
        val year: Int? = null,
    ) {
        companion object {
            fun SuperStreamSearchItem.toSearchResultItem(): SearchResultItem {
                return SearchResultItem(
                    id = id.toString(),
                    title = title,
                    releaseDate = year.toString(),
                    mediaType = SuperStreamUtils.SSMediaType.getSSMediaType(boxType).toMediaType(),
                )
            }
        }
    }
}