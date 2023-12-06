package com.flixclusive.presentation.mobile.screens.search.expanded_content

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.flixclusive.R
import com.flixclusive.presentation.appDestination
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.destinations.SearchFilmScreenDestination
import com.flixclusive.presentation.destinations.SearchScreenContentDestination
import com.flixclusive.presentation.mobile.common.composables.ErrorScreenWithButton
import com.flixclusive.presentation.mobile.common.composables.LARGE_ERROR
import com.flixclusive.presentation.mobile.common.composables.SMALL_ERROR
import com.flixclusive.presentation.mobile.common.composables.film.FilmCard
import com.flixclusive.presentation.mobile.common.composables.film.FilmCardPlaceholder
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.mobile.main.MainMobileSharedViewModel
import com.flixclusive.presentation.mobile.screens.search.SearchNavGraph
import com.flixclusive.presentation.mobile.screens.search.common.SearchFilter
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.utils.LazyListUtils.shouldPaginate
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SearchScreenExpandedTransition : DestinationStyle.Animated {
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
    mainMobileSharedViewModel: MainMobileSharedViewModel
) {
    val viewModel: SearchExpandedViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

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
            viewModel.paginate()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SearchBarExpanded(
                searchQuery = viewModel.searchQuery,
                isError = viewModel.isError,
                currentFilterSelected = viewModel.currentFilterSelected,
                onSearch = viewModel::onSearch,
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
            columns = GridCells.Adaptive(110.dp),
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .graphicsLayer {
                    val topCorner = 4.dp
                    shape = RoundedCornerShape(
                        topEnd = topCorner,
                        topStart = topCorner
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
                    film = film,
                    shouldShowTitle = appSettings.isShowingFilmCardTitle,
                    onClick = { clickedFilm ->
                        navigator.navigate(
                            SearchFilmScreenDestination(
                                film = clickedFilm
                            ),
                            onlyIfResumed = true
                        )
                    },
                    onLongClick = mainMobileSharedViewModel::onFilmLongClick
                )
            }

            if(viewModel.pagingState == PagingState.LOADING || viewModel.pagingState == PagingState.PAGINATING) {
                items(20) {
                    FilmCardPlaceholder(
                        modifier = Modifier
                            .fillMaxSize(),
                        shouldShowTitle = appSettings.isShowingFilmCardTitle
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
                    onRetry = viewModel::paginate
                )
            }
        }
    }
}

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
    fun String.createTextFieldValue(): TextFieldValue {
        return TextFieldValue(
            text = this,
            selection = TextRange(length)
        )
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    var lastSearchedQuery by remember { mutableStateOf(searchQuery) }
    var textFieldValue by remember {
        mutableStateOf(searchQuery.createTextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(searchQuery, lastSearchedQuery) {
        val userIsTypingNewQuery = searchQuery != lastSearchedQuery

        if(userIsTypingNewQuery) {
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
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onQueryChange(textFieldValue.text)
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
                        painter = painterResource(R.drawable.left_arrow),
                        contentDescription = stringResource(R.string.navigate_up)
                    )
                }
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.search_suggestion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorOnMediumEmphasisMobile(),
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
                        onClick = {
                            textFieldValue = "".createTextFieldValue()
                            onQueryChange("")
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close_square),
                            contentDescription = stringResource(R.string.clear_text_button)
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