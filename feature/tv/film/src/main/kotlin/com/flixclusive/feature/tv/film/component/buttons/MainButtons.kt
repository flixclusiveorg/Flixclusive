@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.feature.tv.film.component.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.core.ui.tv.util.hasPressedLeft
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
    goBack: () -> Unit,
) {
    val buttonShape: Shape = MaterialTheme.shapes.extraSmall

    var isPlayButtonFullyFocused by remember { mutableStateOf(true) }

    val nonPlayButtonKeyModifier = Modifier.onPreviewKeyEvent {
        isPlayButtonFullyFocused = false
        false
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayButton(
            watchHistoryItem = watchHistoryItem,
            shape = buttonShape,
            onClick = onPlay,
            modifier = Modifier
                .focusOnMount(PLAY_BUTTON_KEY)
                .onKeyEvent {
                    if (hasPressedLeft(it) && isPlayButtonFullyFocused) {
                        goBack()
                        return@onKeyEvent true
                    } else isPlayButtonFullyFocused = true

                    false
                }
        )

        if (isTvShow) {
            EpisodesButton(
                shape = buttonShape,
                onClick = onSeeMoreEpisodes,
                modifier = nonPlayButtonKeyModifier
                    .focusOnMount(EPISODES_BUTTON_KEY)
            )
        }

        WatchlistButton(
            isInWatchlist = isInWatchlist,
            shape = buttonShape,
            onClick = onWatchlistClick,
            modifier = nonPlayButtonKeyModifier
        )
    }
}

@Preview
@Composable
private fun MainButtonsPreview() {

}