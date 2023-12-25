package com.flixclusive.presentation.mobile.screens.player.controls.dialogs.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.utils.ComposeUtils.applyDropShadow

@Composable
fun PlayerSettingsList(
    modifier: Modifier = Modifier,
    settingsList: List<VideoSettingItem>,
    selectedPanel: VideoSettingItem,
    onPanelChange: (Int) -> Unit,
) {
    LazyColumn(
        modifier = modifier
    ) {
        itemsIndexed(settingsList) { i, item ->
            PlayerSettingsListItem(
                item = item,
                isSelected = selectedPanel == item,
                onPanelChange = { onPanelChange(i) },
            )
        }
    }
}

@Composable
private fun PlayerSettingsListItem(
    modifier: Modifier = Modifier,
    item: VideoSettingItem,
    isSelected: Boolean,
    onPanelChange: () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1F else 0F,
        label = ""
    )
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                colorOnMediumEmphasisMobile(
                    color = Color.Black,
                    emphasis = ELEVATED_VIDEO_SETTINGS_PANEL * alpha,
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = colorOnMediumEmphasisMobile(Color.White, ELEVATED_VIDEO_SETTINGS_PANEL * alpha),
                shape = shape
            )
            .clickable(enabled = !isSelected) {
                onPanelChange()
            }
    ) {
        Row(
            modifier = Modifier
                .height(50.dp)
                .padding(start = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = item.iconId),
                contentDescription = null,
                tint = Color.White.copy(maxOf(alpha, 0.6F))
            )

            Text(
                text = stringResource(item.labelId),
                style = MaterialTheme.typography.labelLarge.applyDropShadow(),
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(maxOf(alpha, 0.6F))
            )
        }
    }
}