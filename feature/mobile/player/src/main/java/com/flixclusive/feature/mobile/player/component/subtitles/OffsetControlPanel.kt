package com.flixclusive.feature.mobile.player.component.subtitles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun OffsetControlPanel(
    hasUnsavedChanges: Boolean,
    currentOffset: Long,
    onOffsetChange: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .padding(15.dp)
    ) {
        Text(
            text = stringResource(id = LocaleR.string.offset),
            style = MaterialTheme.typography.titleMedium
                .asAdaptiveTextStyle()
                .copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { onOffsetChange(currentOffset - 1000) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.keyboard_double_arrow_left_thin),
                    contentDescription = stringResource(LocaleR.string.subtract_1000ms_content_description),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { onOffsetChange(currentOffset - 500) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.chevron_left_thin),
                    contentDescription = stringResource(LocaleR.string.subtract_500ms_content_description),
                    tint = Color.White
                )
            }

            AnimatedContent(
                targetState = currentOffset,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { it / 3 } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "offset_animation",
                modifier = Modifier.weight(1F)
            ) { targetOffset ->
                Text(
                    text = "${targetOffset}ms",
                    style = MaterialTheme.typography.headlineMedium
                        .asAdaptiveTextStyle()
                        .copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                    color = Color.White
                )
            }

            IconButton(
                onClick = { onOffsetChange(currentOffset + 500) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.chevron_right_thin),
                    contentDescription = stringResource(LocaleR.string.add_500ms_content_description),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { onOffsetChange(currentOffset + 1000) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.keyboard_double_arrow_right_thin),
                    contentDescription = stringResource(LocaleR.string.add_1000ms_content_description),
                    tint = Color.White
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            modifier = Modifier.padding(top = 20.dp)
        ) {
            TextButton(
                onClick = { onOffsetChange(0L) },
                enabled = currentOffset != 0L,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.small
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.round_replay_24),
                    contentDescription = stringResource(LocaleR.string.reset),
                    dp = 18.dp,
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = stringResource(LocaleR.string.reset),
                    style = MaterialTheme.typography.bodyMedium
                        .asAdaptiveTextStyle()
                        .copy(fontWeight = FontWeight.Medium)
                )
            }

            OutlinedButton(
                onClick = onSave,
                enabled = hasUnsavedChanges,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.save),
                    contentDescription = stringResource(LocaleR.string.reset),
                    dp = 18.dp,
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = stringResource(LocaleR.string.save),
                    style = MaterialTheme.typography.bodyMedium
                        .asAdaptiveTextStyle()
                        .copy(fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}
