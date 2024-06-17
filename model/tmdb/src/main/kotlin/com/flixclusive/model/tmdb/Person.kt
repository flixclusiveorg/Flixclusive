package com.flixclusive.model.tmdb

import com.flixclusive.core.util.common.ui.UiText
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import com.flixclusive.core.util.R as UtilR

/**
 *
 * Could be a cast or a crew, or just a regular person.
 * */
@Serializable
data class Person(
    val id: Int,
    val name: String,
    val imdbId: String? = null,
    val biography: String? = null,
    val homepage: String? = null,
    val character: String? = null,
    @SerializedName("known_for_department") val knownFor: String? = null,
    @SerializedName("birthday") val birthDay: String? = null,
    @SerializedName("deathday") val deathDay: String? = null,
    @SerializedName("gender") private val rawGender: Int? = null,
    @SerializedName("profile_path") val profilePath: String? = null
) : java.io.Serializable {
    val gender: UiText
        get() {
            return when (rawGender) {
                1 -> UiText.StringResource(UtilR.string.gender_female)
                2 -> UiText.StringResource(UtilR.string.gender_male)
                3 -> UiText.StringResource(UtilR.string.gender_male)
                else -> UiText.StringResource(UtilR.string.gender_none)
            }
        }
}