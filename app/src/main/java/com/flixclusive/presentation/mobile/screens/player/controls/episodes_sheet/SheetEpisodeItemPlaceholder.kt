package com.flixclusive.presentation.mobile.screens.player.controls.episodes_sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.theme.lightGrayElevated
import com.flixclusive.presentation.utils.ModifierUtils.placeholderEffect


@Composable
fun SheetEpisodeItemPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .height(80.dp)
                .padding(horizontal = 10.dp)
                .placeholderEffect()
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .padding(10.dp)
                    .graphicsLayer {
                        shape = RoundedCornerShape(5)
                        clip = true
                    }
                    .drawBehind {
                        drawRect(lightGrayElevated)
                    }
            )

            Box(
                modifier = Modifier
                    .weight(1F)
                    .fillMaxHeight()
                    .padding(
                        top = 10.dp,
                        bottom = 10.dp,
                        end = 10.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(1F)
                            .height(12.dp)
                            .padding(end = 135.dp)
                            .graphicsLayer {
                                shape = RoundedCornerShape(100)
                                clip = true
                            }
                            .drawBehind {
                                drawRect(lightGrayElevated)
                            }
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .padding(end = 20.dp)
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
}