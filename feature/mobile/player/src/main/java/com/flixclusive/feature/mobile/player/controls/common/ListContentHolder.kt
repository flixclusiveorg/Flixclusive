package com.flixclusive.feature.mobile.player.controls.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.ui.player.PlayerProviderState
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.ProviderApi

@Composable
internal fun <Type> ListContentHolder(
    modifier: Modifier = Modifier,
    icon: Painter,
    contentDescription: String?,
    label: String,
    items: List<Type>,
    selectedIndex: Int,
    itemState: PlayerProviderState = PlayerProviderState.SELECTED,
    onItemClick: (Int) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = getFeedbackOnLongPress()

    val listState = rememberLazyListState()
    val listBottomFade = Brush.verticalGradient(
        0.15F to Color.Transparent,
        0.2F to Color.Red,
        0.9F to Color.Red,
        1F to Color.Transparent
    )

    LaunchedEffect(selectedIndex) {
        safeCall { listState.animateScrollToItem(selectedIndex) }
    }

    Box(
        modifier = modifier
            .padding(15.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fadingEdge(listBottomFade),
            state = listState,
            contentPadding = PaddingValues(top = 50.dp, bottom = 15.dp)
        ) {
            itemsIndexed(items) { i, item ->
                val name = when (item) {
                    is String -> item
                    is Stream -> item.name
                    is ProviderApi -> item.provider.name!!
                    is Subtitle -> item.language
                    else -> throw ClassFormatError("Invalid content type provided.")
                }

                ListItem(
                    name = name,
                    index = i,
                    selectedIndex = selectedIndex,
                    itemState = itemState,
                    onClick = {
                        if (i != selectedIndex) {
                            onItemClick(i)
                        }
                    },
                    onLongClick = {
                        if (item is Stream) {
                            hapticFeedback()
                            clipboardManager.setText(
                                AnnotatedString(
                                    """
                                        Stream name: ${item.name}
                                        Stream link: ${item.url}
                                        Stream headers: ${item.customHeaders}
                                    """.trimIndent()
                                )
                            )
                        }
                    }
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
                Icon(
                    painter = icon,
                    contentDescription = contentDescription
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    ),
                )
            }
        }
    }
}