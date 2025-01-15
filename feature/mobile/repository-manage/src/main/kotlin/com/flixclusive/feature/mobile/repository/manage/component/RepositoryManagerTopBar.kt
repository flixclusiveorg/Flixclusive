package com.flixclusive.feature.mobile.repository.manage.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.CommonTopBarWithSearch
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import kotlin.math.max
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun RepositoryManagerTopBar(
    isVisible: Boolean,
    isSelecting: Boolean,
    selectCount: Int,
    onRemoveRepositories: () -> Unit,
    onCopyRepositories: () -> Unit,
    isSearching: Boolean,
    searchQuery: String,
    onCollapseTopBar: () -> Unit,
    onNavigationClick: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
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
                    )
                }

                false -> {
                    CommonTopBarWithSearch(
                        title = stringResource(LocaleR.string.manage_repositories),
                        isSearching = isSearching,
                        searchQuery = searchQuery,
                        onNavigateBack = onNavigationClick,
                        onToggleSearchBar = onToggleSearchBar,
                        onQueryChange = onQueryChange,
                    )
                }
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
    modifier: Modifier = Modifier,
) {
    var count by remember { mutableIntStateOf(selectCount) }
    LaunchedEffect(selectCount) {
        count = max(count, selectCount)
    }

    CommonTopBar(
        boxModifier = modifier,
        navigationIcon = {
            IconButton(onClick = onCollapseTopBar) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.round_close_24),
                    contentDescription = stringResource(LocaleR.string.close_label),
                )
            }
        },
        body = {
            Text(
                text = stringResource(LocaleR.string.count_selection_format, count),
                style =
                    getAdaptiveTextStyle(
                        style = TypographyStyle.Body,
                        mode = TextStyleMode.Normal,
                        size = 20.sp,
                        increaseBy = 5.sp,
                    ).copy(fontWeight = FontWeight.SemiBold),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier =
                    Modifier
                        .weight(1F)
                        .padding(horizontal = 15.dp),
            )
        },
        actions = {
            CustomIconButton(
                description = stringResource(LocaleR.string.copy_repository_links),
                onClick = onCopyLinks,
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.round_content_copy_24),
                    contentDescription = stringResource(LocaleR.string.copy_button),
                )
            }

            CustomIconButton(
                description = stringResource(LocaleR.string.remove),
                onClick = onRemove,
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.outlined_trash),
                    contentDescription = stringResource(LocaleR.string.remove),
                )
            }
        },
    )
}
