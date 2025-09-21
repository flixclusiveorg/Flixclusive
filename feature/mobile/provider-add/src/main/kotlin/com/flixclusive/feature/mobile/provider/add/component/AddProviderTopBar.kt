package com.flixclusive.feature.mobile.provider.add.component

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.components.topbar.CommonTopBarWithSearch
import com.flixclusive.core.presentation.mobile.components.topbar.DefaultNavigationIcon
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun AddProviderTopBar(
    isSearching: Boolean,
    isLoading: Boolean,
    selectCount: Int,
    searchQuery: () -> String,
    onNavigate: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onInstallSelection: () -> Unit,
    onShowFilterSheet: () -> Unit,
    onUnselectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val title =
        if (selectCount > 0) {
            stringResource(LocaleR.string.count_selection_format, selectCount)
        } else {
            stringResource(LocaleR.string.add_providers)
        }

    CommonTopBarWithSearch(
        modifier = modifier,
        isSearching = isSearching,
        title = title,
        onNavigate = onNavigate,
        navigationIcon = {
            if (selectCount > 0) {
                PlainTooltipBox(description = stringResource(LocaleR.string.cancel)) {
                    ActionButton(onClick = onUnselectAll) {
                        AdaptiveIcon(
                            painter = painterResource(UiCommonR.drawable.round_close_24),
                            contentDescription = stringResource(LocaleR.string.cancel),
                        )
                    }
                }
            } else {
                DefaultNavigationIcon(
                    onClick = {
                        if (isSearching) {
                            onToggleSearchBar(false)
                        } else {
                            onNavigate()
                        }
                    },
                )
            }
        },
        searchQuery = searchQuery,
        onToggleSearchBar = onToggleSearchBar,
        onQueryChange = onQueryChange,
        scrollBehavior = scrollBehavior,
        extraActions = {
            if (selectCount > 0 && !isLoading) {
                PlainTooltipBox(description = stringResource(LocaleR.string.install_all)) {
                    ActionButton(onClick = onInstallSelection) {
                        AdaptiveIcon(
                            painter = painterResource(UiCommonR.drawable.download),
                            contentDescription = stringResource(LocaleR.string.install_all),
                            dp = 24.dp,
                        )
                    }
                }
            } else if (!isLoading) {
                PlainTooltipBox(description = stringResource(LocaleR.string.filter_button)) {
                    ActionButton(onClick = onShowFilterSheet) {
                        AdaptiveIcon(
                            painter = painterResource(UiCommonR.drawable.filter_list),
                            contentDescription = stringResource(LocaleR.string.filter_button),
                            dp = 24.dp,
                        )
                    }
                }
            }
        },
    )
}
