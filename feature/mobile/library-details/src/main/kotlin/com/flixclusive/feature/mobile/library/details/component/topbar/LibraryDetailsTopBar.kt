package com.flixclusive.feature.mobile.library.details.component.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.locale.R
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.mobile.component.PlainTooltipBox
import com.flixclusive.core.ui.mobile.component.topbar.ActionButton
import com.flixclusive.core.ui.mobile.component.topbar.DefaultNavigationIcon
import com.flixclusive.core.ui.mobile.component.topbar.EnterOnlyNearTopScrollBehavior
import com.flixclusive.core.ui.mobile.component.topbar.SearchTextFieldAction
import com.flixclusive.feature.mobile.library.common.LibraryTopBarState

@Composable
internal fun LibraryDetailsTopBar(
    topBarState: LibraryTopBarState,
    isListEmpty: Boolean,
    selectCount: () -> Int,
    searchQuery: () -> String,
    onRemoveSelection: () -> Unit,
    onUnselectAll: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    title: @Composable () -> Unit,
    filterContent: @Composable () -> Unit,
    infoContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onGoBack: () -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.largeTopAppBarColors(),
    scrollBehavior: EnterOnlyNearTopScrollBehavior? = null,
) {
    val hideSearchButton =
        (
            topBarState != LibraryTopBarState.DefaultSubScreen &&
            topBarState != LibraryTopBarState.DefaultMainScreen
            ) || isListEmpty

    ThreeRowsTopAppBar(
        title = title,
        titleTextStyle = MaterialTheme.typography.titleLarge,
        modifier = modifier,
        navigationIcon = {
            AnimatedContent(
                targetState = topBarState,
            ) { state ->
                if (state == LibraryTopBarState.Selecting) {
                    PlainTooltipBox(description = stringResource(R.string.cancel)) {
                        ActionButton(onClick = onUnselectAll) {
                            AdaptiveIcon(
                                painter = painterResource(com.flixclusive.core.ui.common.R.drawable.round_close_24),
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    }
                } else if (state == LibraryTopBarState.Searching) {
                    DefaultNavigationIcon(onClick = { onToggleSearchBar(false) })
                } else if (state == LibraryTopBarState.DefaultSubScreen) {
                    DefaultNavigationIcon(onClick = onGoBack)
                }
            }
        },
        actions = {
            SearchTextFieldAction(
                isSearching = topBarState == LibraryTopBarState.Searching,
                hideSearchButton = hideSearchButton,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                onToggleSearchBar = onToggleSearchBar,
                extraActions = {
                    AnimatedVisibility(
                        visible = topBarState == LibraryTopBarState.Selecting && !isListEmpty,
                    ) {
                        PlainTooltipBox(description = stringResource(R.string.remove)) {
                            ActionButton(
                                onClick = onRemoveSelection,
                                enabled = selectCount() > 0,
                            ) {
                                AdaptiveIcon(
                                    painter = painterResource(com.flixclusive.core.ui.common.R.drawable.delete),
                                    contentDescription = stringResource(R.string.remove),
                                    dp = 24.dp,
                                )
                            }
                        }
                    }
                },
            )
        },
        collapsedHeight =
            if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
                TopAppBarDefaults.TopAppBarExpandedHeight
            } else {
                expandedHeight
            },
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
        filterContent = filterContent,
        infoContent = infoContent
    )
}
