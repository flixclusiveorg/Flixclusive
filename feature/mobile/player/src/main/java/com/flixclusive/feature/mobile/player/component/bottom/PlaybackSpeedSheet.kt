package com.flixclusive.feature.mobile.player.component.bottom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.feature.mobile.player.component.bottom.slider.CustomSlider
import com.flixclusive.feature.mobile.player.component.bottom.slider.CustomSliderDefaults
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.drawables.R as UiCommonR


private val Float.toPlayerSpeed: String
    get() = String.format("%.2fx", this)

@Composable
internal fun PlaybackSpeedSheet(
    currentSpeed: Float,
    onSave: (speed: Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val speed by remember {
        derivedStateOf {
            mutableFloatStateOf(currentSpeed)
        }
    }

    Column(
        modifier = modifier
            .fillMaxAdaptiveWidth(
                compact = 0.4f,
                medium = 0.6f,
                expanded = 0.65f
            )
            .clip(MaterialTheme.shapes.small)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(0.3f),
                shape = MaterialTheme.shapes.small
            )
            .drawBehind {
                drawRect(Color.Black)
                drawRect(Color.White.copy(0.1f))
            }
            .padding(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = stringResource(LocaleR.string.playback_speed),
                    style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White
                    )
                )

                Text(
                    text = speed.floatValue.toPlayerSpeed,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.tertiary
                    )
                )
            }

            IconButton(
                onClick = onDismiss
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.round_close_24),
                    contentDescription = stringResource(LocaleR.string.cancel),
                    tint = Color.White.copy(0.7f)
                )
            }
        }

        CustomSlider(
            value = speed.floatValue,
            onValueChange = { speed.floatValue = it },
            valueRange = AppPlayer.playbackSpeedRange,
            interactionSource = interactionSource,
            thumb = {
                CustomSliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(10.dp, 10.dp)
                )
            },
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ProvideTextStyle(
                value = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(0.5f),
                    fontSize = 12.sp
                )
            ) {
                Text(
                    text = AppPlayer.playbackSpeedRange.start.toPlayerSpeed,
                )

                Text(
                    text = AppPlayer.playbackSpeedRange.endInclusive.toPlayerSpeed
                )
            }
        }

        Spacer(
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f),
                onClick = {
                    speed.floatValue = 1.0f
                    onSave(speed.floatValue)
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(LocaleR.string.reset)
                )
            }

            Button(
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f),
                onClick = {
                    onSave(speed.floatValue)
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(LocaleR.string.save)
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun PlaybackSpeedSheetBasePreview() {
    FlixclusiveTheme {
        Surface(
            color = Color(0xFFA25050),
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box {
                PlaybackSpeedSheet(
                    currentSpeed = 1.25f,
                    onSave = {},
                    onDismiss = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5")
@Composable
private fun PlaybackSpeedSheetCompactLandscapePreview() {
    PlaybackSpeedSheetBasePreview()
}

@Preview(device = "spec:parent=medium_tablet")
@Composable
private fun PlaybackSpeedSheetMediumPortraitPreview() {
    PlaybackSpeedSheetBasePreview()
}

@Preview(device = "spec:parent=medium_tablet")
@Composable
private fun PlaybackSpeedSheetMediumLandscapePreview() {
    PlaybackSpeedSheetBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160")
@Composable
private fun PlaybackSpeedSheetExtendedPortraitPreview() {
    PlaybackSpeedSheetBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160")
@Composable
private fun PlaybackSpeedSheetExtendedLandscapePreview() {
    PlaybackSpeedSheetBasePreview()
}
