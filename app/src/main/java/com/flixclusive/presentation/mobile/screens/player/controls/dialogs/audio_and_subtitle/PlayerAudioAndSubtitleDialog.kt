package com.flixclusive.presentation.mobile.screens.player.controls.dialogs.audio_and_subtitle

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import com.flixclusive.R
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.presentation.common.player.FlixclusivePlayer
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.LocalPlayer
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.rememberLocalPlayer
import com.flixclusive.presentation.mobile.screens.player.PlayerSnackbarMessageType
import com.flixclusive.presentation.mobile.screens.player.controls.common.BasePlayerDialog
import com.flixclusive.presentation.mobile.screens.player.controls.common.ListContentHolder
import com.flixclusive.presentation.mobile.screens.player.controls.common.PlayerDialogButton
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.providers.models.common.Subtitle
import com.hippo.unifile.UniFile
import kotlin.random.Random

@Composable
fun PlayerAudioAndSubtitleDialog(
    showSnackbar: (String, Int, PlayerSnackbarMessageType) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val context = LocalContext.current
    val player = rememberLocalPlayer()

    val subtitleFileChooser =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            // It lies, it can be null if file manager quits.
            if (uri == null) return@rememberLauncherForActivityResult

            // RW perms for the path
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val file = UniFile.fromUri(context, uri)
            val fileName = file?.name?.run { "[LOCAL] $this" }

            // DO NOT REMOVE THE FILE EXTENSION FROM NAME, IT'S NEEDED FOR MIME TYPES
            val name = fileName ?: "[LOCAL] Subtitle"
            val filePath = uri.toString()

            val localSub = Subtitle(
                url = filePath,
                lang = name,
            )

            player.addSubtitle(
                uri = filePath,
                subtitle = localSub
            )
            showSnackbar(
                localSub.lang,
                R.string.audio_snackbar_message,
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
                icon = painterResource(id = R.drawable.record_voice_over_black_24dp),
                contentDescription = stringResource(id = R.string.audio),
                label = stringResource(id = R.string.audio),
                items = player.availableAudios,
                selectedIndex = player.selectedAudio,
                onItemClick = {
                    player.run {
                        onAudioChange(it)
                        showSnackbar(
                            availableAudios[it],
                            R.string.audio_snackbar_message,
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
                    .background(colorOnMediumEmphasisMobile(emphasis = 0.4F))
            )

            ListContentHolder(
                modifier = Modifier
                    .weight(1F),
                icon = painterResource(id = R.drawable.outline_subtitles_24),
                contentDescription = stringResource(id = R.string.subtitle),
                label = stringResource(id = R.string.subtitle),
                items = player.availableSubtitles,
                selectedIndex = player.selectedSubtitle,
                onItemClick = {
                    player.run {
                        onSubtitleChange(it)
                        showSnackbar(
                            availableSubtitles[it].language ?: "Default",
                            R.string.audio_snackbar_message,
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
                label = stringResource(id = R.string.add_own_subtitle),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(0.3F),
                    contentColor = Color.White
                ),
                onClick = {
                    subtitleFileChooser.launch(
                        arrayOf(
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
                label = stringResource(R.string.close_label),
                onClick = onDismissSheet
            )
        }
    }
}

@Preview(
    device = "spec:parent=Realme 5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun PlayerAudioAndDisplayDialogPreview() {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val audios = List(10) {
        (1..8)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    val subtitles = List(10) {
        SubtitleConfiguration.Builder(Uri.EMPTY)
            .setLanguage("English #$it")
            .build()
    }

    val context = LocalContext.current
    val player = remember { FlixclusivePlayer(context, AppSettings()) }

    player.availableSubtitles.addAll(subtitles)
    player.availableAudios.addAll(audios)

    CompositionLocalProvider(value = LocalPlayer provides player) {
        FlixclusiveMobileTheme {
            Surface {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sample_movie_subtitle_preview),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                    )

                    PlayerAudioAndSubtitleDialog(showSnackbar = { _, _, _ -> }) {

                    }
                }
            }
        }
    }
}