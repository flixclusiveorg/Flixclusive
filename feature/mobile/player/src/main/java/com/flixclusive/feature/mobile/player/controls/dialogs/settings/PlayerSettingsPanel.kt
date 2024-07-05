package com.flixclusive.feature.mobile.player.controls.dialogs.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.feature.mobile.player.controls.common.ListItem

@Composable
internal fun PlayerSettingsPanel(
    modifier: Modifier = Modifier,
    selected: Int,
    items: List<String>? = null,
    content: (@Composable () -> Unit)? = null,
    onClick: ((Int, String) -> Unit)? = null,
) {

    val listBottomFade = Brush.verticalGradient(
        0F to Color.Transparent,
        0.1F to Color.Red,
        0.9F to Color.Red,
        1F to Color.Transparent
    )

    LazyColumn(
        modifier = modifier
            .fadingEdge(listBottomFade)
    ) {
        items?.let {
            itemsIndexed(items = it) { i, item ->
                ListItem(
                    name = item,
                    index = i,
                    selectedIndex = selected,
                    onClick = {
                        if (i != selected) {
                            onClick?.invoke(i, item)
                        }
                    }
                )
            }
        }

        content?.run {
            item {
                invoke()
            }
        }
    }
}