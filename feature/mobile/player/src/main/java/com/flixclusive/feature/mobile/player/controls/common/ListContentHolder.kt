package com.flixclusive.feature.mobile.player.controls.common

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.ProviderApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <Type> ListContentHolder(
    modifier: Modifier = Modifier,
    icon: Painter,
    contentDescription: String?,
    label: String,
    items: List<Type>,
    selectedIndex: Int,
    itemState: Resource<Any?> = Resource.Success(null),
    onItemClick: (Int) -> Unit,
) {
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
                    is SourceLink -> item.name
                    is ProviderApi -> item.name
                    is Subtitle -> item.language
                    else -> throw ClassFormatError("Invalid content type provided.")
                }

                ListItem(
                    modifier = Modifier
                        .animateItemPlacement(),
                    name = name,
                    index = i,
                    selectedIndex = selectedIndex,
                    itemState = itemState,
                    onClick = {
                        onItemClick(i)
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