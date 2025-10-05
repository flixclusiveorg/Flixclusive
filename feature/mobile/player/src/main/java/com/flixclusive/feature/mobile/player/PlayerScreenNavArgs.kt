package com.flixclusive.feature.mobile.player

import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode

/**
 * Navigation arguments for the PlayerScreen.
 *
 * @param film The film metadata to be played.
 * @param providerId The ID of the provider to be used for fetching links.
 * @param episode The episode to be played (if the film is a TV show).
 * */
data class PlayerScreenNavArgs(
    val film: FilmMetadata,
    val episode: Episode?,
)
