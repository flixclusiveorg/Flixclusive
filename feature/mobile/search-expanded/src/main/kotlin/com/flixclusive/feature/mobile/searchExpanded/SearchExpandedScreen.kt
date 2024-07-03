package com.flixclusive.feature.mobile.searchExpanded

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.navigation.CommonScreenNavigator
import com.flixclusive.core.ui.mobile.util.shouldPaginate
import com.flixclusive.core.util.common.ui.PagingState
import com.flixclusive.feature.mobile.searchExpanded.component.SearchBarInput
import com.flixclusive.feature.mobile.searchExpanded.component.SearchFilmsGridView
import com.flixclusive.feature.mobile.searchExpanded.component.SearchProvidersView
import com.flixclusive.feature.mobile.searchExpanded.component.SearchSearchHistoryView
import com.flixclusive.model.tmdb.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.tmdb.Film
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

internal enum class SearchItemViewType {
    SearchHistory,
    Providers,
    Films;
}

@OptIn(ExperimentalAnimationApi::class)
@Destination
@Composable
fun SearchExpandedScreen(
    navigator: CommonScreenNavigator,
    previewFilm: (Film) -> Unit,
) {
    val viewModel: SearchExpandedScreenViewModel = hiltViewModel()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val shouldStartPaginate by remember {
        derivedStateOf {
            viewModel.canPaginate && listState.shouldPaginate()
        }
    }


    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = shouldStartPaginate) {
        if(shouldStartPaginate && viewModel.pagingState == PagingState.IDLE)
            viewModel.paginateItems()
    }

    val selectedProvider = remember(viewModel.selectedProviderIndex) {
        viewModel.providerDataList
            .getOrNull(viewModel.selectedProviderIndex - 1)
            ?.name ?: DEFAULT_FILM_SOURCE_NAME
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SearchBarInput(
                searchQuery = viewModel.searchQuery,
                currentViewType = viewModel.currentViewType,
                selectedProvider = selectedProvider,
                onSearch = {
                    scope.launch {
                        // Scroll to top
                        listState.scrollToItem(0)
                    }
                    viewModel.onSearch()
                },
                onNavigationIconClick = navigator::goBack,
                onQueryChange = viewModel::onQueryChange,
                onChangeProvider = {
                    viewModel.onChangeProvider(it)
                    scope.launch {
                        listState.scrollToItem(0)
                    }
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = viewModel.currentViewType.value,
            transitionSpec = {
                val enter = when (targetState) {
                    SearchItemViewType.Films -> slideInHorizontally { it } + fadeIn()
                    SearchItemViewType.Providers -> slideInHorizontally { -it } + fadeIn()
                    else -> fadeIn()
                }

                val exit = when (initialState) {
                    SearchItemViewType.Films -> slideOutHorizontally { it } + fadeOut()
                    SearchItemViewType.Providers -> slideOutHorizontally { -it } + fadeOut()
                    else -> fadeOut()
                }

                ContentTransform(
                    targetContentEnter = enter,
                    initialContentExit = exit
                )
            },
            label = ""
        ) { viewType ->
            val modifier = Modifier
                .padding(innerPadding)
                .clip(RoundedCornerShape(topEnd = 4.dp, topStart = 4.dp))

            when (viewType) {
                SearchItemViewType.SearchHistory -> {
                    SearchSearchHistoryView(
                        modifier = modifier,
                        viewModel = viewModel,
                    )
                }
                SearchItemViewType.Providers -> {
                    SearchProvidersView(
                        viewModel = viewModel,
                        modifier = modifier,
                    )
                }
                SearchItemViewType.Films -> {
                    SearchFilmsGridView(
                        modifier = modifier,
                        appSettings = appSettings,
                        listState = listState,
                        previewFilm = previewFilm,
                        viewModel = viewModel,
                        openFilmScreen = navigator::openFilmScreen,
                    )
                }
            }
        }
    }
}