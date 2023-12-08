package com.flixclusive.presentation.mobile.screens.preferences.watchlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.R
import com.flixclusive.presentation.common.FadeInAndOutScreenTransition
import com.flixclusive.presentation.destinations.PreferencesFilmMobileScreenDestination
import com.flixclusive.presentation.mobile.common.composables.FilmsGridScreen
import com.flixclusive.presentation.mobile.main.MainMobileSharedViewModel
import com.flixclusive.presentation.mobile.screens.preferences.PreferencesNavGraph
import com.flixclusive.presentation.mobile.screens.preferences.common.TopBarWithNavigationIcon
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@PreferencesNavGraph
@Destination(
    style = FadeInAndOutScreenTransition::class
)
@Composable
fun WatchlistMobileScreen(
    navigator: DestinationsNavigator,
    mainMobileSharedViewModel: MainMobileSharedViewModel,
) {
    val viewModel: WatchlistViewModel = hiltViewModel()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
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
            Scaffold(
                topBar = {
                    TopBarWithNavigationIcon(
                        modifier = Modifier.align(Alignment.TopStart),
                        headerTitle = stringResource(id = R.string.watchlist),
                        onNavigationIconClick = navigator::navigateUp
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .padding(it)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start adding now!",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = items.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FilmsGridScreen(
                modifier = Modifier.fillMaxSize(),
                screenTitle = stringResource(R.string.watchlist),
                films = items,
                isShowingFilmCardTitle = appSettings.isShowingFilmCardTitle,
                onFilmClick = {
                    navigator.navigate(
                        PreferencesFilmMobileScreenDestination(
                            film = it
                        ),
                        onlyIfResumed = true
                    )
                },
                onNavigationIconClick = navigator::navigateUp,
                onFilmLongClick = mainMobileSharedViewModel::onFilmLongClick,
            )
        }
    }
}