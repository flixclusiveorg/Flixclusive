package com.flixclusive.feature.tv.player.controls.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.core.ui.tv.util.focusOnInitialVisibility
import com.flixclusive.feature.tv.player.controls.settings.common.ConfirmButton
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SubtitleSyncPanel(
    modifier: Modifier = Modifier,
    hidePanel: () -> Unit,
) {
    val player by rememberLocalPlayerManager()
    val oldSubtitleOffset by remember { mutableLongStateOf(player.subtitleOffset) }

    val bigOffsetButton = 140.dp
    val smallOffsetButton = 80.dp
    val topFadeEdge = Brush.verticalGradient(
        0F to Color.Black,
        0.9F to Color.Black.onMediumEmphasis(0.4F)
    )

    BackHandler {
        hidePanel()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(topFadeEdge)
            },
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = LocaleR.string.sync_subtitles),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black
            ),
            color = LocalContentColor.current.onMediumEmphasis(0.5F),
            modifier = Modifier
                .padding(top = 25.dp)
        )

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = LocaleR.string.add_sync_offset_message),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic
                ),
                color = LocalContentColor.current.onMediumEmphasis(0.7F),
            )

            Text(
                text = stringResource(id = LocaleR.string.subtract_sync_offset_message),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic
                ),
                color = LocalContentColor.current.onMediumEmphasis(0.7F),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(0.75F)
                .focusGroup()
        ) {
            OffsetButton(
                drawableId = PlayerR.drawable.keyboard_double_arrow_left_thin,
                contentDescription = stringResource(LocaleR.string.subtract_1000ms_content_description),
                size = smallOffsetButton,
                changeOffset = {
                    player.onSubtitleOffsetChange(player.subtitleOffset - 1000)
                }
            )

            OffsetButton(
                drawableId = PlayerR.drawable.chevron_left_thin,
                contentDescription = stringResource(LocaleR.string.subtract_500ms_content_description),
                size = bigOffsetButton,
                changeOffset = {
                    player.onSubtitleOffsetChange(player.subtitleOffset - 500)
                }
            )

            AnimatedContent(
                targetState = player.subtitleOffset,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { it / 3 } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "",
                modifier = Modifier.weight(1F)
            ) { targetCount ->
                Text(
                    text = "$targetCount",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 80.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }

            OffsetButton(
                drawableId = PlayerR.drawable.chevron_right_thin,
                contentDescription = stringResource(LocaleR.string.add_500ms_content_description),
                size = bigOffsetButton,
                changeOffset = {
                    player.onSubtitleOffsetChange(player.subtitleOffset + 500)
                }
            )

            OffsetButton(
                drawableId = PlayerR.drawable.keyboard_double_arrow_right_thin,
                contentDescription = stringResource(LocaleR.string.add_1000ms_content_description),
                size = smallOffsetButton,
                changeOffset = {
                    player.onSubtitleOffsetChange(player.subtitleOffset + 1000)
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .focusGroup()
        ) {
            ConfirmButton(
                onClick = hidePanel,
                isEmphasis = true,
                label = stringResource(id = LocaleR.string.save)
            )

            ConfirmButton(
                onClick = {
                    player.onSubtitleOffsetChange(oldSubtitleOffset)
                    hidePanel()
                },
                isEmphasis = false,
                label = stringResource(id = LocaleR.string.cancel),
                modifier = Modifier
                    .focusOnInitialVisibility()
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OffsetButton(
    @DrawableRes drawableId: Int,
    contentDescription: String?,
    size: Dp,
    changeOffset: () -> Unit,
) {
    IconButton(
        onClick = changeOffset,
        colors = IconButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White.onMediumEmphasis(0.4F),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White
        ),
        glow = IconButtonDefaults.glow(
            focusedGlow = Glow(
                elevationColor = Color.White.onMediumEmphasis(0.4F),
                elevation = size
            )
        ),
        modifier = Modifier
            .size(size)
    ) {
        Icon(
            painter = painterResource(drawableId),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
        )
    }
}