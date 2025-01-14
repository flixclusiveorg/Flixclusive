package com.flixclusive.feature.mobile.repository.manage.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.CommonTopBarDefaults
import com.flixclusive.core.ui.common.CommonTopBarWithSearch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun RepositoryManagerTopBar(
    isVisible: Boolean,
    isSelecting: Boolean,
    selectCount: Int,
    onRemoveRepositories: () -> Unit,
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
        Box(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter,
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
                            onCollapseTopBar = onCollapseTopBar,
                            modifier = Modifier.height(CommonTopBarDefaults.DefaultTopBarHeight),
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
}

@Composable
private fun MultiSelectTopBar(
    selectCount: Int,
    onRemove: () -> Unit,
    onCollapseTopBar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCollapseTopBar) {
            Icon(
                painter = painterResource(UiCommonR.drawable.round_close_24),
                contentDescription = stringResource(LocaleR.string.close_label),
            )
        }

        Text(
            text = stringResource(LocaleR.string.count_selection_format, selectCount),
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier =
                Modifier
                    .weight(1F)
                    .padding(horizontal = 15.dp),
        )

        IconButton(
            onClick = onRemove,
        ) {
            Icon(
                painter = painterResource(UiCommonR.drawable.outlined_trash),
                contentDescription = stringResource(LocaleR.string.remove),
            )
        }
    }
}
