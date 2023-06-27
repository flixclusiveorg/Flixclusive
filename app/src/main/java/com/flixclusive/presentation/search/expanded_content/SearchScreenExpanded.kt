package com.flixclusive.presentation.search.expanded_content

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.flixclusive.R
import com.flixclusive.presentation.appDestination
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.common.SearchFilter
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.common.composables.ErrorScreenWithButton
import com.flixclusive.presentation.common.composables.LARGE_ERROR
import com.flixclusive.presentation.common.composables.SMALL_ERROR
import com.flixclusive.presentation.common.shouldPaginate
import com.flixclusive.presentation.destinations.SearchFilmScreenDestination
import com.flixclusive.presentation.destinations.SearchScreenContentDestination
import com.flixclusive.presentation.film.FilmCard
import com.flixclusive.presentation.film.FilmCardPlaceholder
import com.flixclusive.presentation.main.LABEL_START_PADDING
import com.flixclusive.presentation.main.MainSharedViewModel
import com.flixclusive.presentation.search.SearchNavGraph
import com.flixclusive.ui.theme.colorOnMediumEmphasis
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyleAnimated
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
object SearchScreenExpandedTransition : DestinationStyleAnimated {
    override fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition(): EnterTransition? {
        return when(initialState.appDestination()) {
            SearchScreenContentDestination -> null
            else -> fadeIn()
        }
    }

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition(): ExitTransition = fadeOut()
}

@SearchNavGraph
@Destination(
    style = SearchScreenExpandedTransition::class
)
@Composable
fun SearchScreenExpanded(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel
) {
    val viewModel: SearchExpandedViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()

    val errorHeight = remember(viewModel.searchResults) {
        if(viewModel.searchResults.isEmpty()) {
            LARGE_ERROR
        } else {
            SMALL_ERROR
        }
    }

    val shouldStartPaginate by remember {
        derivedStateOf {
            viewModel.canPaginate && listState.shouldPaginate()
        }
    }

    LaunchedEffect(key1 = shouldStartPaginate) {
        if(shouldStartPaginate && viewModel.pagingState == PagingState.IDLE)
            viewModel.getSearchItems()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SearchBarExpanded(
                searchQuery = viewModel.searchQuery,
                isError = viewModel.isError,
                currentFilterSelected = viewModel.currentFilterSelected,
                onSearch = viewModel::onSearchClick,
                onNavigationIconClick = navigator::navigateUp,
                onErrorChange = viewModel::onErrorChange,
                onQueryChange = viewModel::onQueryChange,
                onFilterChange = {
                    viewModel.onChangeFilter(it)
                    scope.launch {
                        listState.scrollToItem(0)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(108.dp),
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .graphicsLayer {
                    val topCornerPercentage = 5
                    shape = RoundedCornerShape(
                        topEndPercent = topCornerPercentage,
                        topStartPercent = topCornerPercentage
                    )
                    clip = true
                }
        ) {
            itemsIndexed(
                items = viewModel.searchResults,
                key = { i, film ->
                    film.id * i
                }
            ) { _, film ->
                FilmCard(
                    modifier = Modifier
                        .fillMaxSize(),
                    shouldShowTitle = false,
                    film = film,
                    onClick = { clickedFilm ->
                        navigator.navigate(
                            SearchFilmScreenDestination(
                                film = clickedFilm
                            ),
                            onlyIfResumed = true
                        )
                    },
                    onLongClick = mainSharedViewModel::onFilmLongClick
                )
            }

            if(viewModel.pagingState == PagingState.LOADING || viewModel.pagingState == PagingState.PAGINATING) {
                items(20) {
                    FilmCardPlaceholder(
                        modifier = Modifier
                            .fillMaxSize(),
                        shouldShowTitle = false
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorScreenWithButton(
                    modifier = Modifier
                        .height(errorHeight)
                        .fillMaxWidth(),
                    shouldShowError = viewModel.pagingState == PagingState.PAGINATING_EXHAUST || viewModel.pagingState == PagingState.ERROR,
                    error = stringResource(R.string.error_on_search),
                    onRetry = viewModel::getSearchItems
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                )
            }
        }
    }
}

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun SearchBarExpanded(
    searchQuery: String = "",
    currentFilterSelected: SearchFilter,
    isError: Boolean = false,
    onSearch: () -> Unit,
    onNavigationIconClick: () -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onErrorChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var lastSearchedQuery by remember { mutableStateOf(searchQuery) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(searchQuery, lastSearchedQuery) {
        val queryIsNotEmpty = searchQuery.isNotEmpty()
        val userIsTypingNewQuery = searchQuery != lastSearchedQuery

        if(queryIsNotEmpty && userIsTypingNewQuery) {
            delay(1500L)
            onSearch()
        }

        lastSearchedQuery = searchQuery
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = LABEL_START_PADDING,
                    end = LABEL_START_PADDING,
                    top = 10.dp
                )
                .focusRequester(focusRequester),
            value = searchQuery,
            onValueChange = {
                onQueryChange(it)
                onErrorChange(false)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()

                    if(searchQuery.isEmpty())
                        onErrorChange(true)

                    lastSearchedQuery = searchQuery // Update last searched query to avoid re-searching again with the LaunchedEffect scope
                    onSearch()
                }
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(55),
            colors = TextFieldDefaults.colors(
                disabledTextColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            leadingIcon = {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        painter = IconResource.fromDrawableResource(R.drawable.left_arrow)
                            .asPainterResource(),
                        contentDescription = UiText.StringResource(R.string.navigate_up).asString()
                    )
                }
            },
            placeholder = {
                Text(
                    text = UiText.StringResource(R.string.search_suggestion).asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorOnMediumEmphasis(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            },
            supportingText = {
                if (isError) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Search query should not be empty!",
                        color = MaterialTheme.colorScheme.error,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            },
            trailingIcon = {
                this@Column.AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    IconButton(
                        onClick = { onQueryChange("") }
                    ) {
                        Icon(
                            painter = IconResource.fromDrawableResource(R.drawable.close_square)
                                .asPainterResource(),
                            contentDescription = UiText.StringResource(R.string.clear_text_button)
                                .asString()
                        )
                    }
                }
            },
        )

        SearchFiltersButtons(
            currentFilterSelected = currentFilterSelected,
            onFilterChange = onFilterChange
        )
    }
}