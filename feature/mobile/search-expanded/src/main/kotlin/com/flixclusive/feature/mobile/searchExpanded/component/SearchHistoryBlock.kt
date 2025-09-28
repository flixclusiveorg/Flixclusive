package com.flixclusive.feature.mobile.searchExpanded.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchHistoryBlock(
    item: SearchHistory,
    onClick: () -> Unit,
    onArrowClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AdaptiveIcon(
                painter = painterResource(id = UiCommonR.drawable.time_circle_outlined),
                contentDescription = stringResource(LocaleR.string.search_history_icon),
                tint = LocalContentColor.current.copy(0.6f),
                dp = 16.dp,
            )

            Text(
                text = item.query,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge.asAdaptiveTextStyle(14.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1F)
                    .padding(horizontal = 10.dp)
            )

            Box(modifier = Modifier.clickable { onArrowClick() }) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.north_west_arrow),
                    contentDescription = stringResource(LocaleR.string.search_history_icon),
                    tint = LocalContentColor.current.copy(0.8F),
                    dp = 20.dp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SearchHistoryBlockPreview() {
    FlixclusiveTheme {
        Surface {
            SearchHistoryBlock(
                item = SearchHistory(
                    query = "test",
                    ownerId = 0,
                ),
                onClick = {},
                onArrowClick = {},
                onLongClick = {}
            )
        }
    }
}
