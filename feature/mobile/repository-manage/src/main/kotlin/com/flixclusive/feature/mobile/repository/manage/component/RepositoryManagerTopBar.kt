package com.flixclusive.feature.mobile.repository.manage.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarWithSearch
import kotlin.math.max
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun RepositoryManagerTopBar(
    isSelecting: Boolean,
    selectCount: Int,
    onRemoveRepositories: () -> Unit,
    onCopyRepositories: () -> Unit,
    isSearching: Boolean,
    searchQuery: () -> String,
    onCollapseTopBar: () -> Unit,
    onNavigationClick: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    Crossfade(
        targetState = isSelecting,
        label = "",
    ) {
        when (it) {
            true -> {
                MultiSelectTopBar(
                    selectCount = selectCount,
                    onRemove = onRemoveRepositories,
                    onCopyLinks = onCopyRepositories,
                    onCollapseTopBar = onCollapseTopBar,
                    scrollBehavior = scrollBehavior,
                )
            }

            false -> {
                CommonTopBarWithSearch(
                    title = stringResource(LocaleR.string.manage_repositories),
                    isSearching = isSearching,
                    searchQuery = searchQuery,
                    onNavigate = onNavigationClick,
                    onToggleSearchBar = onToggleSearchBar,
                    onQueryChange = onQueryChange,
                    scrollBehavior = scrollBehavior,
                )
            }
        }
    }
}

@Composable
private fun MultiSelectTopBar(
    selectCount: Int,
    onRemove: () -> Unit,
    onCopyLinks: () -> Unit,
    onCollapseTopBar: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    var count by remember { mutableIntStateOf(selectCount) }
    LaunchedEffect(selectCount) {
        count = max(count, selectCount)
    }

    CommonTopBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = onCollapseTopBar) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.round_close_24),
                    contentDescription = stringResource(LocaleR.string.close_label),
                )
            }
        },
        title = {
            Text(
                text = stringResource(LocaleR.string.count_selection_format, count),
                style = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(
                    size = 20.sp,
                    increaseBy = 5.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier =
                    Modifier
                        .padding(horizontal = 15.dp),
            )
        },
        actions = {
            PlainTooltipBox(description = stringResource(LocaleR.string.navigate_up)) {
                ActionButton(onClick = onCopyLinks) {
                    AdaptiveIcon(
                        painter = painterResource(UiCommonR.drawable.round_content_copy_24),
                        contentDescription = stringResource(LocaleR.string.copy_button),
                    )
                }
            }

            PlainTooltipBox(description = stringResource(LocaleR.string.remove)) {
                ActionButton(onClick = onRemove) {
                    AdaptiveIcon(
                        painter = painterResource(UiCommonR.drawable.outlined_trash),
                        contentDescription = stringResource(LocaleR.string.remove),
                    )
                }
            }
        },
    )
}
