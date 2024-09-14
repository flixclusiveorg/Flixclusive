package com.flixclusive.feature.tv.player.controls.settings.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.PlayerProviderState
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.focusOnInitialVisibility
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.ProviderApi

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun <Type> ListContentHolder(
    modifier: Modifier = Modifier,
    contentDescription: String?,
    label: String,
    items: List<Type>,
    selectedIndex: Int,
    itemState: PlayerProviderState = PlayerProviderState.SELECTED,
    initializeFocus: Boolean = false,
    onItemClick: (Int) -> Unit,
) {
    val listState = rememberTvLazyListState()
    val listBottomFade = Brush.verticalGradient(
        0.1F to Color.Transparent,
        0.18F to Color.Red,
        0.7F to Color.Red,
        1F to Color.Transparent
    )

    val isFirstItemFocusedLaunched = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        safeCall { listState.animateScrollToItem(selectedIndex) }
    }

    Box(
        modifier = modifier
            .padding(15.dp)
    ) {
        TvLazyColumn(
            modifier = Modifier
                .fadingEdge(listBottomFade),
            pivotOffsets = PivotOffsets(0.2F),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterVertically),
            contentPadding = PaddingValues(vertical = 15.dp)
        ) {
            item {
                NonFocusableSpacer(height = 35.dp)
            }

            itemsIndexed(items) { i, item ->
                val name = when (item) {
                    is String -> item
                    is Stream -> item.name
                    is ProviderApi -> item.provider.name!!
                    is Subtitle -> item.language
                    else -> throw ClassFormatError("Invalid content type provided.")
                }

                ListItem(
                    modifier = Modifier
                        .animateItemPlacement()
                        .ifElse(
                            condition = initializeFocus && i == selectedIndex && !isFirstItemFocusedLaunched.value,
                            ifTrueModifier = Modifier.focusOnInitialVisibility(
                                isFirstItemFocusedLaunched
                            )
                        ),
                    name = name,
                    index = i,
                    selectedIndex = selectedIndex,
                    itemState = itemState,
                    onClick = {
                        onItemClick(i)
                    }
                )
            }

            items(10) {
                NonFocusableSpacer(height = 50.dp)
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            ),
            color = LocalContentColor.current.onMediumEmphasis(0.4F),
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        )
    }
}