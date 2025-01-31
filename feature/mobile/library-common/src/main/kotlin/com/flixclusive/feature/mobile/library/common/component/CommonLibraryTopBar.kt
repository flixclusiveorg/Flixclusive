package com.flixclusive.feature.mobile.library.common.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.PlainTooltipBox
import com.flixclusive.core.ui.mobile.component.topbar.ActionButton
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarWithSearch
import com.flixclusive.core.ui.mobile.component.topbar.DefaultNavigationIcon
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

enum class CommonLibraryTopBarState {
    DefaultMainScreen,
    DefaultSubScreen,
    Selecting,
    Searching,
}

@Composable
fun CommonLibraryTopBar(
    topBarState: CommonLibraryTopBarState,
    scrollBehavior: TopAppBarScrollBehavior,
    isListEmpty: Boolean,
    selectCount: () -> Int,
    searchQuery: () -> String,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onRemoveSelection: () -> Unit,
    onStartMultiSelecting: () -> Unit,
    onShowFilterSheet: () -> Unit,
    onUnselectAll: () -> Unit,
    modifier: Modifier = Modifier,
    onGoBack: () -> Unit = {},
    titleContent: @Composable () -> Unit,
) {
    val hideSearchButton =
        (
            topBarState != CommonLibraryTopBarState.DefaultSubScreen &&
                topBarState != CommonLibraryTopBarState.DefaultMainScreen
        ) ||
            isListEmpty

    CommonTopBarWithSearch(
        modifier = modifier,
        isSearching = topBarState == CommonLibraryTopBarState.Searching,
        titleContent = titleContent,
        onNavigate = onGoBack,
        navigationIcon = {
            AnimatedContent(
                targetState = topBarState,
            ) { state ->
                if (state == CommonLibraryTopBarState.Selecting) {
                    PlainTooltipBox(description = stringResource(LocaleR.string.cancel)) {
                        ActionButton(onClick = onUnselectAll) {
                            AdaptiveIcon(
                                painter = painterResource(UiCommonR.drawable.round_close_24),
                                contentDescription = stringResource(LocaleR.string.cancel),
                            )
                        }
                    }
                } else if (state == CommonLibraryTopBarState.Searching) {
                    DefaultNavigationIcon(onClick = { onToggleSearchBar(false) })
                } else if (state == CommonLibraryTopBarState.DefaultSubScreen) {
                    DefaultNavigationIcon(onClick = onGoBack)
                }
            }
        },
        searchQuery = searchQuery,
        onToggleSearchBar = onToggleSearchBar,
        onQueryChange = onQueryChange,
        scrollBehavior = scrollBehavior,
        hideSearchButton = hideSearchButton,
        extraActions = {
            AnimatedContent(
                targetState = topBarState,
            ) { state ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state == CommonLibraryTopBarState.Selecting && !isListEmpty) {
                        PlainTooltipBox(description = stringResource(LocaleR.string.remove)) {
                            ActionButton(
                                onClick = onRemoveSelection,
                                enabled = selectCount() > 0,
                            ) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.delete),
                                    contentDescription = stringResource(LocaleR.string.remove),
                                    dp = 24.dp,
                                )
                            }
                        }
                    } else if (!isListEmpty) {
                        PlainTooltipBox(description = stringResource(LocaleR.string.filter_button)) {
                            ActionButton(onClick = onShowFilterSheet) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.filter_list),
                                    contentDescription = stringResource(LocaleR.string.filter_button),
                                    dp = 24.dp,
                                )
                            }
                        }

                        PlainTooltipBox(description = stringResource(LocaleR.string.multi_select)) {
                            ActionButton(onClick = onStartMultiSelecting) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.select),
                                    contentDescription = stringResource(LocaleR.string.multi_select),
                                    tint = LocalContentColor.current.onMediumEmphasis(0.8F),
                                    dp = 24.dp,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
