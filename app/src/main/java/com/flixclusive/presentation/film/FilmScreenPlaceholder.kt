package com.flixclusive.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.common.composables.fadingEdge
import com.flixclusive.presentation.common.composables.placeholderEffect
import com.flixclusive.presentation.main.LABEL_START_PADDING
import com.flixclusive.ui.theme.lightGray
import com.flixclusive.ui.theme.lightGrayElevated
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilmScreenPlaceholder(
    modifier: Modifier = Modifier,
) {
    val listBottomFade = Brush.verticalGradient(0.8f to Color.Red, 0.9f to Color.Transparent)

    Column(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .height(500.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(listBottomFade)
                    .drawBehind {
                        drawRect(lightGray)
                    }
            ) {}

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LABEL_START_PADDING,
                        vertical = LABEL_START_PADDING
                    )
                    .align(Alignment.BottomCenter)
            ) {
                Spacer(
                    modifier = Modifier
                        .height(55.dp)
                        .fillMaxWidth(0.5F)
                        .graphicsLayer {
                            shape = RoundedCornerShape(25)
                            clip = true
                        }
                        .drawBehind {
                            drawRect(lightGrayElevated)
                        }
                )

                Spacer(
                    modifier = Modifier
                        .height(25.dp)
                        .fillMaxWidth(0.4F)
                        .padding(top = 15.dp)
                        .graphicsLayer {
                            shape = RoundedCornerShape(100)
                            clip = true
                        }
                        .drawBehind {
                            drawRect(lightGrayElevated)
                        }
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    maxItemsInEachRow = 3
                ) {
                    repeat(3) {
                        val randomWidth by remember { mutableStateOf(Random.nextInt(60, 120)) }

                        Spacer(
                            modifier = Modifier
                                .height(23.dp)
                                .width(randomWidth.dp)
                                .graphicsLayer {
                                    shape = RoundedCornerShape(100)
                                    clip = true
                                }
                                .drawBehind {
                                    drawRect(lightGrayElevated)
                                }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = LABEL_START_PADDING)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 25.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier = Modifier
                        .height(50.dp)
                        .width(250.dp)
                        .placeholderEffect(shape = RoundedCornerShape(100))
                )

                Spacer(
                    modifier = Modifier
                        .size(50.dp)
                        .placeholderEffect(shape = RoundedCornerShape(15))
                )
            }

            repeat(6) {
                val widthFraction = remember {
                    when (it) {
                        0, 1 -> 1F
                        5 -> 0.4F
                        else -> Random.nextDouble(0.95, 1.0).toFloat()
                    }
                }
                Spacer(
                    modifier = Modifier
                        .height(17.dp)
                        .padding(vertical = 3.dp)
                        .fillMaxWidth(widthFraction)
                        .placeholderEffect(shape = RoundedCornerShape(100))
                )
            }
        }
    }
}