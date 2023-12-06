package com.flixclusive.presentation.mobile.screens.player.controls.video_settings_dialog

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.flixclusive.presentation.mobile.screens.player.controls.common.SheetItem
import com.flixclusive.presentation.utils.ModifierUtils.fadingEdge

@Composable
fun VideoSettingsPanel(
    modifier: Modifier = Modifier,
    items: List<String>,
    selected: Int,
    onClick: (Int, String) -> Unit,
) {

    val listBottomFade = Brush.verticalGradient(
        0.9F to Color.Red,
        1F to Color.Transparent
    )

    LazyColumn(
        modifier = modifier
            .fadingEdge(listBottomFade)
    ) {
        itemsIndexed(items = items) { i, item ->
            SheetItem(
                name = item,
                index = i,
                selectedIndex = selected,
                onClick = {
                    onClick(i, item)
                }
            )
        }
    }
}