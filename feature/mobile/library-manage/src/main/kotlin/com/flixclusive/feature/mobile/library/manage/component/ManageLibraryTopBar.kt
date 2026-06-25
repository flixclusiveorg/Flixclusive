package com.flixclusive.feature.mobile.library.manage.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.DefaultNavigationIcon
import com.flixclusive.core.presentation.mobile.components.material3.topbar.SearchTextFieldAction
import com.flixclusive.core.presentation.mobile.components.material3.topbar.TwoRowsTopAppBar
import com.flixclusive.feature.mobile.library.common.LibraryTopBarState
import com.flixclusive.feature.mobile.library.manage.R
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ManageLibraryTopBar(
    topBarState: LibraryTopBarState,
    isListEmpty: Boolean,
    enableTrackerButton: () -> Boolean,
    selectCount: () -> Int,
    searchQuery: () -> String,
    onShowTrackers: () -> Unit,
    onRemoveSelection: () -> Unit,
    onUnselectAll: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val hideSearchButton = remember(topBarState, isListEmpty) {
        (
            topBarState != LibraryTopBarState.DefaultSubScreen &&
                topBarState != LibraryTopBarState.DefaultMainScreen
        ) ||
            isListEmpty
    }

    BackHandler(enabled = topBarState == LibraryTopBarState.Selecting) {
        onUnselectAll()
    }

    TwoRowsTopAppBar(
        title = title,
        titleTextStyle = MaterialTheme.typography.titleLarge,
        modifier = modifier,
        navigationIcon = {
            AnimatedContent(
                targetState = topBarState,
            ) { state ->
                if (state == LibraryTopBarState.Selecting) {
                    PlainTooltipBox(description = stringResource(LocaleR.string.label_cancel)) {
                        ActionButton(onClick = onUnselectAll) {
                            AdaptiveIcon(
                                painter = painterResource(UiCommonR.drawable.round_close_24),
                                contentDescription = stringResource(LocaleR.string.label_cancel),
                            )
                        }
                    }
                } else if (state == LibraryTopBarState.Searching) {
                    DefaultNavigationIcon(onClick = { onToggleSearchBar(false) })
                } else if (state == LibraryTopBarState.DefaultSubScreen) {
                    DefaultNavigationIcon(onClick = {})
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
                    AnimatedContent(
                        targetState = topBarState == LibraryTopBarState.Selecting && !isListEmpty,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) { state ->
                        if (state) {
                            PlainTooltipBox(description = stringResource(LocaleR.string.remove)) {
                                ActionButton(
                                    onClick = onRemoveSelection,
                                    enabled = selectCount() > 0,
                                ) {
                                    AdaptiveIcon(
                                        painter = painterResource(UiCommonR.drawable.outlined_trash),
                                        contentDescription = stringResource(LocaleR.string.remove),
                                        dp = 24.dp,
                                    )
                                }
                            }
                        } else {
                            PlainTooltipBox(description = stringResource(R.string.trackers)) {
                                ActionButton(
                                    onClick = onShowTrackers,
                                    enabled = enableTrackerButton(),
                                ) {
                                    AdaptiveIcon(
                                        painter = painterResource(UiCommonR.drawable.trackers),
                                        contentDescription = stringResource(R.string.trackers),
                                        dp = 24.dp,
                                    )
                                }
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
        content = content,
    )
}
