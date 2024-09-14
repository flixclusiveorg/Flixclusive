package com.flixclusive.model.film

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

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
) : java.io.Serializable