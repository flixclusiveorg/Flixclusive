package com.flixclusive.feature.mobile.player.component.common

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.model.track.PlayerSubtitle
import com.flixclusive.core.presentation.player.model.track.PlayerTrack
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.provider.ProviderMetadata

@Composable
internal fun <Type> ListContentHolder(
    items: Collection<Type>,
    icon: Painter,
    contentDescription: String?,
    label: String,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    failedIndices: Set<Int> = emptySet(),
    onItemLongClick: (Int) -> Unit = {},
    actions: @Composable RowScope.() -> Unit = { }
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        safeCall { listState.animateScrollToItem(selectedIndex) }
    }

    Box(
        modifier = modifier
            .padding(15.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fadingEdge(
                scrollableState = listState,
                orientation = Orientation.Vertical,
                startEdge = 100.dp,
                endEdge = 100.dp,
            ),
            state = listState,
            contentPadding = PaddingValues(top = 50.dp, bottom = 15.dp)
        ) {
            items(
                count = items.size,
                key = {
                    when (val item = items.elementAt(it)) {
                        is String -> item
                        is ProviderMetadata -> item.id
                        is PlayerServer -> item.url + item.label
                        is PlayerSubtitle -> item.url + item.label
                        else -> throw ClassFormatError("Invalid content type provided.")
                    }
                }
            ) { i ->
                val name = when (val item = items.elementAt(i)) {
                    is String -> item
                    is ProviderMetadata -> item.name
                    is PlayerTrack -> item.label
                    else -> throw ClassFormatError("Invalid content type provided.")
                }

                ListItem(
                    name = name,
                    isSelected = i == selectedIndex,
                    isFailed = remember { derivedStateOf { i in failedIndices } }.value,
                    onClick = { onItemClick(i) },
                    onLongClick = { onItemLongClick(i) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdaptiveIcon(
                    painter = icon,
                    contentDescription = contentDescription
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ).asAdaptiveTextStyle(size = 22.sp),
                    modifier = Modifier.weight(1f)
                )

                actions()
            }

            HorizontalDivider(
                color = Color.White.copy(0.15f),
                thickness = 0.5.dp,
            )
        }
    }
}
