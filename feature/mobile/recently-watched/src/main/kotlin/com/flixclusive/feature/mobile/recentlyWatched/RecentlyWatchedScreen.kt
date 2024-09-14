package com.flixclusive.feature.mobile.recentlyWatched

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.navigator.CommonScreenNavigator
import com.flixclusive.core.ui.mobile.component.film.FilmsGridScreen
import com.flixclusive.model.film.Film
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR

@Destination
@Composable
internal fun RecentlyWatchedScreen(
    navigator: CommonScreenNavigator,
    previewFilm: (Film) -> Unit,
) {
    val viewModel = hiltViewModel<RecentlyWatchedScreenViewModel>()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val watchHistoryItems by viewModel.items.collectAsStateWithLifecycle()

    Box {
        AnimatedVisibility(
            visible = watchHistoryItems.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
        ) {
            Scaffold(
                topBar = {
                    CommonTopBar(
                        modifier = Modifier.align(Alignment.TopStart),
                        headerTitle = stringResource(id = LocaleR.string.recently_watched),
                        onNavigationIconClick = navigator::goBack
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
                        text = stringResource(id = LocaleR.string.empty_recently_watched_list_message),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = watchHistoryItems.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FilmsGridScreen(
                modifier = Modifier.fillMaxSize(),
                screenTitle = stringResource(LocaleR.string.recently_watched),
                films = watchHistoryItems,
                isShowingFilmCardTitle = appSettings.isShowingFilmCardTitle,
                onFilmClick = navigator::openFilmScreen,
                onNavigationIconClick = navigator::goBack,
                onFilmLongClick = previewFilm,
            )
        }
    }
}