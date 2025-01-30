package com.flixclusive.feature.mobile.library.manage.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

internal enum class TopBarNavigationState {
    Default,
    Selecting,
    Searching,
}

@Composable
internal fun ManageLibraryTopBar(
    navigationState: TopBarNavigationState,
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
) {
    val context = LocalContext.current
    val title =
        if (navigationState == TopBarNavigationState.Selecting) {
            context.getString(LocaleR.string.count_selection_format, selectCount())
        } else {
            context.getString(LocaleR.string.my_library)
        }

    CommonTopBarWithSearch(
        modifier = modifier,
        isSearching = navigationState == TopBarNavigationState.Searching,
        title = title,
        onNavigate = {},
        navigationIcon = {
            AnimatedContent(
                targetState = navigationState,
            ) { state ->
                if (state == TopBarNavigationState.Selecting) {
                    PlainTooltipBox(description = stringResource(LocaleR.string.cancel)) {
                        ActionButton(onClick = onUnselectAll) {
                            AdaptiveIcon(
                                painter = painterResource(UiCommonR.drawable.round_close_24),
                                contentDescription = stringResource(LocaleR.string.cancel),
                            )
                        }
                    }
                } else if (state == TopBarNavigationState.Searching) {
                    DefaultNavigationIcon(
                        onClick = { onToggleSearchBar(false) },
                    )
                }
            }
        },
        searchQuery = searchQuery,
        onToggleSearchBar = onToggleSearchBar,
        onQueryChange = onQueryChange,
        scrollBehavior = scrollBehavior,
        hideSearchButton = navigationState != TopBarNavigationState.Default || isListEmpty,
        extraActions = {
            AnimatedContent(
                targetState = navigationState,
            ) { state ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state == TopBarNavigationState.Selecting && !isListEmpty) {
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
