package com.flixclusive.feature.mobile.player.controls.dialogs.audio_and_subtitle

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.PlayerSnackbarMessageType
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.feature.mobile.player.controls.common.BasePlayerDialog
import com.flixclusive.feature.mobile.player.controls.common.ListContentHolder
import com.flixclusive.feature.mobile.player.controls.common.PlayerDialogButton
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource
import com.hippo.unifile.UniFile
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR


@OptIn(UnstableApi::class)
@Composable
internal fun PlayerAudioAndSubtitleDialog(
    showSnackbar: (String, Int, PlayerSnackbarMessageType) -> Unit,
    addSubtitle: (Subtitle) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val context = LocalContext.current
    val player by rememberLocalPlayerManager()

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

            val localSub = Subtitle(
                url = filePath,
                language = name,
                type = SubtitleSource.LOCAL
            )

            addSubtitle(localSub)
            player.preferredSubtitleLanguage = name

            showSnackbar(
                localSub.language,
                LocaleR.string.subtitle_snackbar_message,
                PlayerSnackbarMessageType.Audio
            )
        }

    BasePlayerDialog(onDismissSheet = onDismissSheet) {
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
                items = player.availableAudios,
                selectedIndex = player.selectedAudioIndex,
                onItemClick = {
                    player.run {
                        onAudioChange(it)
                        showSnackbar(
                            availableAudios[it],
                            LocaleR.string.audio_snackbar_message,
                            PlayerSnackbarMessageType.Audio
                        )
                    }
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 10.dp)
                    .fillMaxHeight(0.9F)
                    .width(0.5.dp)
                    .background(LocalContentColor.current.onMediumEmphasis(emphasis = 0.4F))
            )

            ListContentHolder(
                modifier = Modifier
                    .weight(1F),
                icon = painterResource(id = PlayerR.drawable.outline_subtitles_24),
                contentDescription = stringResource(id = LocaleR.string.subtitle_icon_content_desc),
                label = stringResource(id = LocaleR.string.subtitle),
                items = player.availableSubtitles,
                selectedIndex = player.selectedSubtitleIndex,
                onItemClick = {
                    player.run {
                        onSubtitleChange(it)
                        showSnackbar(
                            availableSubtitles[it].language,
                            LocaleR.string.subtitle_snackbar_message,
                            PlayerSnackbarMessageType.Audio
                        )
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
        ) {
            PlayerDialogButton(
                label = stringResource(id = LocaleR.string.add_own_subtitle),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(0.3F),
                    contentColor = Color.White
                ),
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
            )

            PlayerDialogButton(
                label = stringResource(LocaleR.string.close_label),
                onClick = onDismissSheet
            )
        }
    }
}
