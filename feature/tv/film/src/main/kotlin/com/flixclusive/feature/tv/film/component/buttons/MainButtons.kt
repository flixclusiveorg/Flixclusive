@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.feature.tv.film.component.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.tv.util.createInitialFocusRestorerModifiers
import com.flixclusive.core.ui.tv.util.focusOnInitialVisibility
import com.flixclusive.model.database.WatchHistoryItem

@Composable
internal fun MainButtons(
    watchHistoryItem: WatchHistoryItem?,
    isInWatchlist: Boolean,
    isTvShow: Boolean,
    shouldFocusOnPlayButton: Boolean,
    shouldFocusOnEpisodesButton: MutableState<Boolean>,
    onPlay: () -> Unit,
    onWatchlistClick: () -> Unit,
    onSeeMoreEpisodes: () -> Unit,
) {
    val buttonShape: Shape = MaterialTheme.shapes.extraSmall
    val focusRestorerModifiers = createInitialFocusRestorerModifiers()

    Row(
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = focusRestorerModifiers.parentModifier
    ) {
        PlayButton(
            watchHistoryItem = watchHistoryItem,
            shape = buttonShape,
            onClick = onPlay,
            modifier = focusRestorerModifiers.childModifier
                .ifElse(
                    condition = shouldFocusOnPlayButton,
                    ifTrueModifier = Modifier.focusOnInitialVisibility()
                )
        )

        if (isTvShow) {
            EpisodesButton(
                shape = buttonShape,
                onClick = onSeeMoreEpisodes,
                modifier = Modifier.focusOnInitialVisibility(isVisible = shouldFocusOnEpisodesButton)
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