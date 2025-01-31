package com.flixclusive.feature.mobile.library.common.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import kotlinx.collections.immutable.ImmutableList
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
fun LibraryFilterBottomSheet(
    filters: ImmutableList<LibrarySortFilter>,
    currentFilter: LibrarySortFilter,
    currentDirection: LibraryFilterDirection,
    onUpdateFilter: (LibrarySortFilter) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape =
            MaterialTheme.shapes.small.copy(
                bottomEnd = CornerSize(0.dp),
                bottomStart = CornerSize(0.dp),
            ),
        dragHandle = { DragHandle() },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(LocaleR.string.sort_by),
                        style =
                            getAdaptiveTextStyle(
                                mode = TextStyleMode.Emphasized,
                                style = TypographyStyle.Headline,
                            ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(
                items = filters,
                key = { it.displayName.hashCode() },
            ) {
                LibraryFilter(
                    isSelected = currentFilter == it,
                    filter = it,
                    direction = currentDirection,
                    onClick = { onUpdateFilter(it) }
                )
            }
        }
    }
}

@Composable
private fun LibraryFilter(
    isSelected: Boolean,
    filter: LibrarySortFilter,
    direction: LibraryFilterDirection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AnimatedContent(
                targetState = direction,
                label = "SortDirection",
                modifier =
                    Modifier
                        .padding(horizontal = 5.dp)
                        .align(Alignment.CenterVertically),
            ) { state ->
                if (isSelected) {
                    val iconId =
                        when (state.isAscending) {
                            true -> UiCommonR.drawable.sort_ascending
                            else -> UiCommonR.drawable.sort_descending
                        }

                    AdaptiveIcon(
                        painter = painterResource(iconId),
                        contentDescription = stringResource(LocaleR.string.sort_icon_content_desc),
                        dp = 16.dp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }

            Text(
                text = filter.displayName.asString(),
                style =
                    getAdaptiveTextStyle(
                        mode = TextStyleMode.SemiEmphasized,
                        size = 15.sp,
                    ).copy(
                        color = LocalContentColor.current.onMediumEmphasis(if (isSelected) 1F else 0.6F),
                    ),
                modifier =
                    Modifier
                        .weight(1F)
                        .align(Alignment.CenterVertically),
            )
        }
    }
}
