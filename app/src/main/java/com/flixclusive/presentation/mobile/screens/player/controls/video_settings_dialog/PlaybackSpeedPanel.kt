package com.flixclusive.presentation.mobile.screens.player.controls.video_settings_dialog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.presentation.mobile.screens.player.controls.common.SheetItem
import com.flixclusive.presentation.utils.ComposeUtils.applyDropShadow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaybackSpeedPanel(
    modifier: Modifier = Modifier,
    selectedPlaybackSpeed: Int,
    onPlaybackSpeedChange: (index: Int) -> Unit,
) {
    LazyColumn(
        modifier = modifier
    ) {
        stickyHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(0.4F))
                    .height(40.dp)
                    .padding(start = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.spedometer),
                    contentDescription = "Playback speed icon header"
                )

                Text(
                    text = stringResource(R.string.playback_speed),
                    style = MaterialTheme.typography.labelLarge.applyDropShadow(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        items(5) { index ->
            val itemName = remember {
                if (index == 0) "Normal"
                else "${1F + (index * 0.25F)}x"
            }

            SheetItem(
                name = itemName,
                index = index,
                selectedIndex = selectedPlaybackSpeed,
                onClick = { onPlaybackSpeedChange(index) }
            )
        }


    }
}