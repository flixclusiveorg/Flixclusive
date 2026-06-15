package com.flixclusive.feature.tv.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewMediaAction
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.home.HomeScreenViewModel
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.LocalFocusTransferredOnLaunchProvider
import com.flixclusive.core.ui.tv.util.getLocalDrawerWidth
import com.flixclusive.core.ui.tv.util.shouldPaginate
import com.flixclusive.core.ui.tv.util.useLocalCurrentRoute
import com.flixclusive.core.ui.tv.util.useLocalLastFocusedItemPerDestination
import com.flixclusive.feature.tv.home.component.HOME_FOCUS_KEY_FORMAT
import com.flixclusive.feature.tv.home.component.HomeMediasRow
import com.flixclusive.feature.tv.home.component.ImmersiveHomeBackground
import com.flixclusive.feature.tv.home.component.util.LocalImmersiveBackgroundColorProvider
import com.flixclusive.feature.tv.home.component.util.useLocalImmersiveBackgroundColor
import com.flixclusive.feature.tv.home.component.watched.HOME_WATCHED_FILMS_FOCUS_KEY_FORMAT
import com.flixclusive.feature.tv.home.component.watched.HomeContinueWatchingRow
import com.flixclusive.model.media.MediaMetadata
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.delay
import java.util.Locale

interface HomeScreenTvNavigator :
    ViewMediaAction,
    GoBackAction {
    fun openPlayerScreen(media: MediaMetadata)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Destination<ExternalModuleGraph>
@Composable
internal fun HomeScreen(
    navigator: HomeScreenTvNavigator
,viewModel: HomeScreenViewModel = hiltViewModel()) {
    val currentRoute = useLocalCurrentRoute()
    val lastItemFocused = useLocalLastFocusedItemPerDestination()

    val listState = rememberTvLazyListState()
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val headerItem = uiState.headerItem
    val homeCategories = uiState.catalogs
    val homeRowItemsPagingState = uiState.rowItemsPagingState
    val homeRowItems = uiState.rowItems
    val continueWatchingList by viewModel.continueWatchingList.collectAsStateWithLifecycle()

    var focusedMedia: MediaMetadata? by remember { mutableStateOf(null) }
    var focusedOnWatchedMedias by rememberSaveable { mutableStateOf(continueWatchingList.isNotEmpty()) }

    val fadeFloat by animateFloatAsState(
        targetValue = if (focusedOnWatchedMedias) 0F else 0.5F,
        label = ""
    )
    val surface = MaterialTheme.colorScheme.surface

    // Initialize focus to the first item.
    LaunchedEffect(Unit) {
        val defaultValue = if (continueWatchingList.isNotEmpty()) {
            String.format(Locale.ROOT, HOME_WATCHED_FILMS_FOCUS_KEY_FORMAT, 0, 0)
        } else {
            String.format(Locale.ROOT, HOME_FOCUS_KEY_FORMAT, 1, 0)
        }

        lastItemFocused.getOrPut(currentRoute) {
            // Pre-load the focused media if there's no watched medias.
            homeRowItems
                .getOrNull(0)
                ?.getOrNull(0)
                ?.let {
                    viewModel.loadFocusedMedia(it)
                }

            defaultValue
        }
    }

    LaunchedEffect(focusedMedia) {
        delay(800)
        focusedMedia?.let {
            viewModel.loadFocusedMedia(it)
        }
    }

    LocalFocusTransferredOnLaunchProvider {
        LocalImmersiveBackgroundColorProvider {
            val backgroundColor = useLocalImmersiveBackgroundColor()

            LaunchedEffect(focusedOnWatchedMedias, lastItemFocused[currentRoute]) {
                // Check if there's a `watched` term on the last
                // Focused item before the screen got unfocused.
                // If true, then don't show the immersive background yet.
                // See [HOME_WATCHED_FILMS_FOCUS_KEY_FORMAT]
                focusedOnWatchedMedias = focusedOnWatchedMedias ||
                    lastItemFocused[currentRoute]?.contains("watched", true) == true

                // Remove the custom color if watched medias aren't focused anymore
                if (!focusedOnWatchedMedias) {
                    backgroundColor.value = null
                }
            }

            val animatedBackgroundColor by animateColorAsState(
                targetValue = backgroundColor.value ?: surface,
                animationSpec = tween(durationMillis = 800),
                label = ""
            )

            Box(
                modifier = Modifier
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    animatedBackgroundColor,
                                    surface
                                ),
                                center = Offset(
                                    x = size.width,
                                    y = size.height
                                ),
                                radius = size.width.times(0.85F)
                            )
                        )

                        drawRect(surface.copy(alpha = 0.6F))
                    }
            ) {
                AnimatedVisibility(
                    visible = !focusedOnWatchedMedias,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ImmersiveHomeBackground(
                        headerItem = headerItem,
                        modifier = Modifier
                            .padding(start = getLocalDrawerWidth())
                    )
                }

                Box(
                    modifier = Modifier
                        .fadingEdge(
                            brush = Brush.verticalGradient(
                                (fadeFloat - 0.02F).coerceAtLeast(0F) to Color.Transparent,
                                fadeFloat to Color.Red
                            )
                        )
                ) {
                    if (!uiState.status.isLoading) {
                        val shouldStartPaginate by remember {
                            derivedStateOf {
                                listState.shouldPaginate()
                            }
                        }

                        LaunchedEffect(shouldStartPaginate) {
                            if (shouldStartPaginate) {
                                viewModel.onPaginateCatalogs()
                            }
                        }

                        TvLazyColumn(
                            pivotOffsets = PivotOffsets(if (focusedOnWatchedMedias) 0.15F else 0.55F),
                            state = listState,
                        ) {
                            if (continueWatchingList.isNotEmpty()) {
                                item {
                                    HomeContinueWatchingRow(
                                        items = continueWatchingList,
                                        onPlayClick = navigator::openPlayerScreen,
                                        modifier = Modifier
                                            .padding(bottom = 20.dp)
                                            .onFocusChanged {
                                                focusedOnWatchedMedias = it.hasFocus
                                            }
                                    )
                                }
                            } else {
                                items(3) {
                                    NonFocusableSpacer(height = 110.dp)
                                }
                            }

                            items(
                                count = viewModel.itemsSize,
                                key = { it % homeCategories.size }
                            ) { i ->
                                val rowIndex = i % homeCategories.size
                                val catalog = homeCategories[rowIndex]

                                HomeMediasRow(
                                    catalogItem = catalog,
                                    paginationState = homeRowItemsPagingState[rowIndex],
                                    medias = homeRowItems[rowIndex],
                                    rowIndex = i + if (continueWatchingList.isNotEmpty()) 1 else 0,
                                    onMediaClick = navigator::openMediaScreen,
                                    onFocusedMediaChange = { media ->
                                        focusedMedia = media
                                    },
                                    paginate = { page ->
                                        viewModel.onPaginateMedias(
                                            catalog = catalog,
                                            page = page,
                                            index = i
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
