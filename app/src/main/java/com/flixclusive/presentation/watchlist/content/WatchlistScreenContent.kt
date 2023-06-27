package com.flixclusive.presentation.watchlist.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.R
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.common.composables.VerticalGridScreen
import com.flixclusive.presentation.common.transitions.ChildScreenTransition
import com.flixclusive.presentation.destinations.WatchlistFilmScreenDestination
import com.flixclusive.presentation.main.LABEL_START_PADDING
import com.flixclusive.presentation.main.MainSharedViewModel
import com.flixclusive.presentation.watchlist.WatchlistNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@WatchlistNavGraph(start = true)
@Destination(
    style = ChildScreenTransition::class
)
@Composable
fun WatchlistScreenContent(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
) {
    val viewModel: WatchlistContentViewModel = hiltViewModel()
    val watchHistoryItems by viewModel.items.collectAsStateWithLifecycle()
    val items = remember(watchHistoryItems) {
        watchHistoryItems
            .map { it.film }
    }

    Box {
        AnimatedVisibility(
            visible = items.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = LABEL_START_PADDING),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = UiText.StringResource(R.string.watchlist).asString(),
                            style = MaterialTheme.typography.headlineMedium,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = "Your list is empty!",
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(
            visible = items.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VerticalGridScreen(
                modifier = Modifier.fillMaxSize(),
                screenTitle = UiText.StringResource(R.string.watchlist).asString(),
                films = items,
                onFilmClick = {
                    navigator.navigate(
                        WatchlistFilmScreenDestination(
                            film = it
                        ),
                        onlyIfResumed = true
                    )
                },
                onFilmLongClick = mainSharedViewModel::onFilmLongClick,
            )
        }
    }
}