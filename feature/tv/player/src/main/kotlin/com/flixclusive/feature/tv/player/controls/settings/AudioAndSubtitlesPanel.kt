package com.flixclusive.feature.tv.player.controls.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.R
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.core.ui.tv.util.hasPressedLeft
import com.flixclusive.feature.tv.player.controls.settings.common.ListContentHolder

@Composable
internal fun AudioAndSubtitlesPanel(
    modifier: Modifier = Modifier,
    hidePanel: () -> Unit,
) {
    val player by rememberLocalPlayerManager()


    var isFirstItemFullyFocused by remember { mutableStateOf(true) }

    val blackBackgroundGradient = Brush.horizontalGradient(
        0F to Color.Black.onMediumEmphasis(0.4F),
        0.15F to Color.Black.onMediumEmphasis(),
        1F to Color.Black
    )

    BackHandler {
        hidePanel()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(blackBackgroundGradient),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85F)
        ) {
            ListContentHolder(
                modifier = Modifier
                    .weight(1F)
                    .onKeyEvent {
                        if (hasPressedLeft(it) && isFirstItemFullyFocused) {
                            hidePanel()
                            return@onKeyEvent true
                        } else isFirstItemFullyFocused = true

                        false
                    },
                initializeFocus = true,
                icon = painterResource(id = R.drawable.record_voice_over_black_24dp),
                contentDescription = stringResource(id = com.flixclusive.core.util.R.string.audio_icon_content_desc),
                label = stringResource(id = com.flixclusive.core.util.R.string.audio),
                items = player.availableAudios,
                selectedIndex = player.selectedAudio,
                onItemClick = player::onAudioChange
            )

            ListContentHolder(
                modifier = Modifier
                    .weight(1F)
                    .onPreviewKeyEvent {
                        isFirstItemFullyFocused = false
                        false
                    },
                icon = painterResource(id = R.drawable.outline_subtitles_24),
                contentDescription = stringResource(id = com.flixclusive.core.util.R.string.subtitle_icon_content_desc),
                label = stringResource(id = com.flixclusive.core.util.R.string.subtitle),
                items = player.availableSubtitles,
                selectedIndex = player.selectedSubtitleIndex,
                onItemClick = player::onSubtitleChange
            )
        }
    }
}

