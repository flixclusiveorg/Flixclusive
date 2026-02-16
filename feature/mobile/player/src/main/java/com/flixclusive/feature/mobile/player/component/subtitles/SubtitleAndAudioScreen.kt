package com.flixclusive.feature.mobile.player.component.subtitles

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.common.extensions.noOpClickable
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.core.presentation.player.ui.state.TracksState
import com.flixclusive.feature.mobile.player.component.common.ListContentHolder
import com.hippo.unifile.UniFile
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(UnstableApi::class)
@Composable
internal fun SubtitleAndAudioScreen(
    tracksState: TracksState,
    onSyncSubtitles: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    BackHandler {
        onDismiss()
    }

    val subtitleLabel = stringResource(id = LocaleR.string.subtitle)
    val subtitleFileSelector =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null)
                return@rememberLauncherForActivityResult

            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.contentResolver.takePersistableUriPermission(
                /* uri = */ uri,
                /* modeFlags = */ flags
            )

            val file = UniFile.fromUri(context, uri)
            val fileName = file?.name?.run { "[LOCAL] $this" }

            // DO NOT REMOVE THE FILE EXTENSION FROM NAME, IT'S NEEDED FOR MIME TYPES
            val name = fileName ?: "[LOCAL] Subtitle"
            val filePath = uri.toString()

            val localSub = MediaSubtitle(
                url = filePath,
                label = name,
                source = TrackSource.LOCAL
            )

            tracksState.onAddSubtitle(localSub)
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.9F))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .noOpClickable()
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(end = 20.dp)
                    .align(Alignment.End)
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.round_close_24),
                    contentDescription = stringResource(id = LocaleR.string.close),
                    tint = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight(0.85F)
            ) {
                ListContentHolder(
                    modifier = Modifier
                        .weight(1F),
                    icon = painterResource(id = PlayerR.drawable.record_voice_over_black_24dp),
                    contentDescription = stringResource(id = LocaleR.string.audio_icon_content_desc),
                    label = stringResource(id = LocaleR.string.audio),
                    items = tracksState.audios,
                    selectedIndex = tracksState.selectedAudio,
                    onItemClick = tracksState::onAudioSelect
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 10.dp)
                        .fillMaxHeight(0.9F)
                        .width(0.5.dp)
                        .background(LocalContentColor.current.copy(alpha = 0.4F))
                )

                ListContentHolder(
                    modifier = Modifier
                        .weight(1F),
                    icon = painterResource(id = UiCommonR.drawable.outline_subtitles_24),
                    contentDescription = stringResource(id = LocaleR.string.subtitle_icon_content_desc),
                    label = subtitleLabel,
                    items = tracksState.subtitles,
                    selectedIndex = tracksState.selectedSubtitle,
                    onItemClick = tracksState::onSubtitleSelect,
                    onItemLongClick = {
                        val item = tracksState.subtitles[it]
                        val data = """
                            Subtitle label: ${item.label}
                            Subtitle source: ${item.source}
                            Subtitle URL: ${item.url}
                        """.trimIndent()

                        clipboardManager.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText("${item.label} - ${item.source}", data)
                        )
                    },
                    actions = {
                        IconButton(onClick = onSyncSubtitles) {
                            AdaptiveIcon(
                                painter = painterResource(id = PlayerR.drawable.sync_black_24dp),
                                contentDescription = stringResource(id = PlayerR.string.sync_subtitles)
                            )
                        }

                        IconButton(
                            onClick = {
                                subtitleFileSelector.launch(
                                    arrayOf(
                                        "text/plain",
                                        "text/str",
                                        "application/octet-stream",
                                        MimeTypes.TEXT_UNKNOWN,
                                        MimeTypes.TEXT_VTT,
                                        MimeTypes.TEXT_SSA,
                                        MimeTypes.APPLICATION_TTML,
                                        MimeTypes.APPLICATION_MP4VTT,
                                        MimeTypes.APPLICATION_SUBRIP,
                                    )
                                )
                            }
                        ) {
                            AdaptiveIcon(
                                painter = painterResource(id = UiCommonR.drawable.round_add_24),
                                contentDescription = stringResource(id = PlayerR.string.add_subtitle)
                            )
                        }
                    }
                )
            }
        }
    }
}
