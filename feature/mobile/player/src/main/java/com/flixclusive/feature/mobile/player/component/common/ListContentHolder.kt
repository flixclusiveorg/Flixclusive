package com.flixclusive.feature.mobile.player.component.common

import android.content.ClipData
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.ProviderApi

@Composable
internal fun <Type> ListContentHolder(
    items: List<Type>,
    icon: Painter,
    contentDescription: String?,
    label: String,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
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
                edgeSize = 100.dp,
            ),
            state = listState,
            contentPadding = PaddingValues(top = 50.dp, bottom = 15.dp)
        ) {
            itemsIndexed(items) { i, item ->
                val name = when (item) {
                    is String -> item
                    is Stream -> item.name
                    is ProviderApi -> item.provider.name
                    is Subtitle -> item.language
                    else -> throw ClassFormatError("Invalid content type provided.")
                }

                ListItem(
                    name = name,
                    isSelected = i == selectedIndex,
                    onClick = { onItemClick(i) },
                    onLongClick = { onItemLongClick(i) }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
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
        }
    }
}
