package com.flixclusive.feature.mobile.player.component.gesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.player.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerVerticalSlider(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    iconPainter: @Composable () -> Painter,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.4F)
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .fillMaxHeight(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 30.dp),
        ) {
            Icon(
                painter = iconPainter(),
                contentDescription = null,
                tint = Color.White
            )

            Slider(
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = 270f
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxHeight,
                            )
                        )

                        layout(placeable.height, placeable.width) {
                            placeable.place(-placeable.width, 0)
                        }
                    }
                    .width(120.dp),
                valueRange = valueRange,
                value = value,
                onValueChange = onValueChange,
                colors = sliderColors,
                track = {
                     SliderDefaults.Track(
                         sliderState = it,
                         drawStopIndicator = {},
                         drawTick = { _, _ -> },
                         thumbTrackGapSize = 0.dp,
                         modifier = Modifier
                             .height(5.dp)
                    )
                },
                thumb = {}
            )
        }
    }
}


@Preview(
    device = "spec:parent=pixel_5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun VerticalSliderPreview() {
    val sliderIconId = R.drawable.brightness_full
    val (value, onValueChange) = remember {
        mutableFloatStateOf(0.3F)
    }

    FlixclusiveTheme {
        Surface {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.Red),
            ) {
                PlayerVerticalSlider(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                0F to MaterialTheme.colorScheme.onSurface.copy(0.6F),
                                0.9F to Color.Transparent,
                            )
                        )
                        .padding(end = 150.dp)
                        .align(Alignment.CenterStart),
                    isVisible = true,
                    iconPainter = { painterResource(sliderIconId) },
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = 0F..1F
                )

                PlayerVerticalSlider(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                0F to Color.Transparent,
                                0.9F to MaterialTheme.colorScheme.onSurface.copy(0.6F),
                            )
                        )
                        .padding(start = 150.dp)
                        .align(Alignment.CenterEnd),
                    isVisible = true,
                    iconPainter = { painterResource(sliderIconId) },
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = 0F..1F
                )
            }
        }
    }
}
