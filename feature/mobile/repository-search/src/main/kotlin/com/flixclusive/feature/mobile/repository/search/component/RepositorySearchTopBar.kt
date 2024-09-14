package com.flixclusive.feature.mobile.repository.search.component

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun RepositorySearchTopBar(
    isVisible: Boolean,
    isSelecting: MutableState<Boolean>,
    selectCount: Int,
    onRemoveRepositories: () -> Unit,
    onNavigationIconClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .height(65.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Crossfade(
                targetState = isSelecting.value,
                label = ""
            ) {
                when(it) {
                    true -> {
                        ExpandedTopBar(
                            selectCount = selectCount,
                            onRemove = onRemoveRepositories,
                            onCollapseTopBar = { isSelecting.value = false }
                        )
                    }
                    false -> {
                        CollapsedTopBar(onNavigationIconClick = onNavigationIconClick,)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedTopBar(
    modifier: Modifier = Modifier,
    onNavigationIconClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigationIconClick) {
            Icon(
                painter = painterResource(UiCommonR.drawable.left_arrow),
                contentDescription = stringResource(LocaleR.string.navigate_up)
            )
        }

        Text(
            text = stringResource(id = LocaleR.string.add_provider),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .weight(1F)
                .padding(start = 15.dp)
        )
    }
}

@Composable
private fun ExpandedTopBar(
    modifier: Modifier = Modifier,
    selectCount: Int,
    onRemove: () -> Unit,
    onCollapseTopBar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCollapseTopBar) {
            Icon(
                painter = painterResource(UiCommonR.drawable.round_close_24),
                contentDescription = stringResource(LocaleR.string.close_label)
            )
        }

        Text(
            text = stringResource(LocaleR.string.count_selection_format, selectCount),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .weight(1F)
                .padding(horizontal = 15.dp)
        )

        IconButton(
            onClick = onRemove
        ) {
            Icon(
                painter = painterResource(UiCommonR.drawable.delete),
                contentDescription = stringResource(LocaleR.string.remove)
            )
        }
    }
}