package com.flixclusive.presentation.mobile.screens.player.controls

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer

@Composable
fun TopControls(
    modifier: Modifier = Modifier,
    onNavigationIconClick: () -> Unit,
    onVideoSettingsClick: () -> Unit,
) {
    val player = LocalPlayer.current

    val topFadeEdge = Brush.verticalGradient(0F to Color.Black, 0.9F to Color.Transparent)

    Box(
        modifier = modifier
            .drawBehind {
                drawRect(brush = topFadeEdge)
            }
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp)
    ) {
        TopControlsButton(
            modifier = Modifier
                .align(Alignment.CenterStart),
            iconId = R.drawable.left_arrow,
            contentDescription = "Back button",
            onClick = onNavigationIconClick
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85F),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (player?.mediaMetadata?.displayTitle ?: "").toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopControlsButton(
                iconId = R.drawable.settings,
                contentDescription = "Playback speed button",
                onClick = onVideoSettingsClick
            )
        }
    }
}

@Composable
private fun TopControlsButton(
    modifier: Modifier = Modifier,
    @DrawableRes iconId: Int,
    contentDescription: String?,
    size: Dp = 65.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .background(color = Color.Transparent)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = false,
                    radius = size / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = contentDescription,
            tint = Color.White
        )
    }
}