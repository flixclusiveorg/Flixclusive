package com.flixclusive.feature.mobile.settings.screen.links.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.topbar.DefaultNavigationIcon
import com.flixclusive.core.presentation.mobile.components.material3.topbar.TwoRowsTopAppBar
import com.flixclusive.feature.mobile.settings.R
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun MediaLinkCardsTweakTopBar(
    mediaSort: MediaSortType,
    navigateBack: () -> Unit,
    onMediaSortChange: (MediaSortType) -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val sorts = remember {
        listOf(
            MediaSortType.LinksCount::class,
            MediaSortType.Title::class
        )
    }

    TwoRowsTopAppBar(
        title = { Text(stringResource(LocaleR.string.label_cached_links)) },
        titleTextStyle = MaterialTheme.typography.titleLarge,
        navigationIcon = { DefaultNavigationIcon(onClick = navigateBack) },
        actions = {},
        collapsedHeight = TopAppBarDefaults.TopAppBarExpandedHeight,
        windowInsets = TopAppBarDefaults.windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(),
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            sorts.fastForEach {
                val isSelected = mediaSort::class == it

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onMediaSortChange(
                            if (isSelected) {
                                mediaSort.toggle()
                            } else {
                                mediaSort.changeType()
                            }
                        )
                    },
                    leadingIcon = {
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AnimatedContent(targetState = mediaSort.asc) { state ->
                                val iconId = if (state) {
                                    UiCommonR.drawable.sort_ascending
                                } else {
                                    UiCommonR.drawable.sort_descending
                                }

                                AdaptiveIcon(
                                    painter = painterResource(iconId),
                                    contentDescription = stringResource(
                                        LocaleR.string.sort_icon_content_desc
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    dp = 14.dp,
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            when (it) {
                                MediaSortType.LinksCount::class -> stringResource(
                                    R.string.label_links_filter_count
                                )

                                MediaSortType.Title::class -> stringResource(
                                    R.string.label_links_filter_title
                                )

                                else -> "Unknown filter"
                            }
                        )
                    },
                )
            }
        }
    }
}
