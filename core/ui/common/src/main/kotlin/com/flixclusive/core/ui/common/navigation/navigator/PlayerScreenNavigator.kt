package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode

interface PlayerScreenNavigator : GoBackAction {
    /**
     *
     * There will be cases where the system will kill
     * the app's process mercilessly if it's never put as an exclusion
     * in the **ignore battery optimization list**.
     *
     * When the user comes back to the app after a long while,
     * it will scrape back the old data that were saved from SavedStateHandle.
     *
     * This action should trigger the `savedStateHandle` to
     * update its values based on PlayerScreenNavArgs - since that is where
     * the args are saved on.
     *
     * So whenever the user comes back to the app, the last episode the user
     * was on would be re-used.
     * */
    fun onEpisodeChange(
        film: Film,
        episodeToPlay: Episode
    )
}