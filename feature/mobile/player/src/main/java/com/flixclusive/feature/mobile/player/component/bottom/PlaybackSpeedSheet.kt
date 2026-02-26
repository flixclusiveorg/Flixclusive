package com.flixclusive.feature.mobile.player.component.bottom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.GlassSurface
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.state.PlaybackSpeedState
import com.flixclusive.feature.mobile.player.component.bottom.slider.CustomSlider
import com.flixclusive.feature.mobile.player.component.bottom.slider.CustomSliderDefaults
import java.util.Locale
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR


private val Float.toPlayerSpeed: String
    get() = String.format(Locale.ROOT, "%.2fx", this)


@Composable
internal fun PlaybackSpeedSheet(
    playbackSpeedState: PlaybackSpeedState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.small
    val interactionSource = remember { MutableInteractionSource() }
    val speed by remember {
        derivedStateOf {
            mutableFloatStateOf(playbackSpeedState.playbackSpeed)
        }
    }

    BackHandler {
        onDismiss()
    }

    GlassSurface(
        shape = shape,
        modifier = modifier
            .fillMaxAdaptiveWidth(
                compact = 0.4f,
                medium = 0.6f,
                expanded = 0.65f
            ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = stringResource(PlayerR.string.speed),
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
                    ).asAdaptiveTextStyle(size = 12.sp)
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
                        playbackSpeedState.updatePlaybackSpeed(speed.floatValue)
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
}
