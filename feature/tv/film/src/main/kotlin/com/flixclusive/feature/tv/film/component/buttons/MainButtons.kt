@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.feature.tv.film.component.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.model.database.WatchHistoryItem

internal const val PLAY_BUTTON_KEY = "play_button"
internal const val EPISODES_BUTTON_KEY = "episodes_button"

@Composable
internal fun MainButtons(
    watchHistoryItem: WatchHistoryItem?,
    isInWatchlist: Boolean,
    isTvShow: Boolean,
    onPlay: () -> Unit,
    onWatchlistClick: () -> Unit,
    onSeeMoreEpisodes: () -> Unit,
) {
    val buttonShape: Shape = MaterialTheme.shapes.extraSmall

    Row(
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayButton(
            watchHistoryItem = watchHistoryItem,
            shape = buttonShape,
            onClick = onPlay,
            modifier = Modifier
                .focusOnMount(PLAY_BUTTON_KEY)
        )

        if (isTvShow) {
            EpisodesButton(
                shape = buttonShape,
                onClick = onSeeMoreEpisodes,
                modifier = Modifier
                    .focusOnMount(EPISODES_BUTTON_KEY)
            )
        }

        WatchlistButton(
            isInWatchlist = isInWatchlist,
            shape = buttonShape,
            onClick = onWatchlistClick
        )
    }
}

@Preview
@Composable
private fun MainButtonsPreview() {

}