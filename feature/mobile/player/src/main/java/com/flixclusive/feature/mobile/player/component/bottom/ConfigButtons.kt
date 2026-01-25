package com.flixclusive.feature.mobile.player.component.bottom

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun ConfigButtons(
    onLock: () -> Unit,
    onShowSpeedPanel: () -> Unit,
    onShowCcPanel: () -> Unit,
    onShowServersPanel: () -> Unit,
    onShowSubtitleSyncPanel: () -> Unit,
    modifier: Modifier = Modifier,
    onShowEpisodesPanel: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
    ) {
        onShowEpisodesPanel?.let { onClick ->
            ConfigButton(
                icon = painterResource(PlayerR.drawable.outline_video_library_24),
                contentDescription = stringResource(LocaleR.string.episodes),
                onClick = onClick
            )
        }

        ConfigButton(
            icon = painterResource(PlayerR.drawable.gauge),
            contentDescription = stringResource(LocaleR.string.playback_speed),
            onClick = onShowSpeedPanel
        )

        ConfigButton(
            icon = painterResource(PlayerR.drawable.record_voice_over_black_24dp),
            contentDescription = stringResource(LocaleR.string.audio_and_subtitle),
            onClick = onShowCcPanel
        )

        ConfigButton(
            icon = painterResource(PlayerR.drawable.round_cloud_queue_24),
            contentDescription = stringResource(LocaleR.string.servers),
            onClick = onShowServersPanel
        )

        ConfigButton(
            icon = painterResource(PlayerR.drawable.sync_black_24dp),
            contentDescription = stringResource(LocaleR.string.sync_subtitles),
            onClick = onShowSubtitleSyncPanel
        )

        ConfigButton(
            icon = painterResource(UiCommonR.drawable.lock_thin),
            contentDescription = stringResource(LocaleR.string.lock),
            onClick = onLock
        )
    }
}

@Composable
private fun ConfigButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlainTooltipBox(
        description = contentDescription,
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier
        ) {
            AdaptiveIcon(
                painter = icon,
                contentDescription = contentDescription
            )
        }
    }
}
