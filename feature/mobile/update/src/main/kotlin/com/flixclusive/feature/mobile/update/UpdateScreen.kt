package com.flixclusive.feature.mobile.update

import android.text.util.Linkify
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.StartHomeScreenAction
import com.flixclusive.core.ui.common.navigation.navargs.UpdateScreenNavArgs
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.util.android.installApkActivity
import com.flixclusive.feature.mobile.update.util.fromGitmoji
import com.flixclusive.service.update.AppUpdaterService
import com.flixclusive.service.update.AppUpdaterService.Companion.startAppUpdater
import com.ramcosta.composedestinations.annotation.Destination
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.flixclusive.core.locale.R as LocaleR

@Destination(
    navArgsDelegate = UpdateScreenNavArgs::class
)
@Composable
internal fun UpdateScreen(
    navigator: StartHomeScreenAction,
    args: UpdateScreenNavArgs,
) {
    val downloadProgress by AppUpdaterService.downloadProgress.collectAsStateWithLifecycle()
    val apkUri by AppUpdaterService.installUriLocation.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val buttonPaddingValues = PaddingValues(horizontal = 5.dp, vertical = 10.dp)
    val brushGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        )
    )
    val progressColor = MaterialTheme.colorScheme.primary

    val progressState by animateFloatAsState(
        targetValue = downloadProgress?.div(100F) ?: 0F,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = ""
    )
    val readyToInstall = remember(downloadProgress, apkUri) {
        downloadProgress != null && apkUri != null && downloadProgress!! >= 100
    }

    BackHandler {
        if (args.isComingFromSplashScreen) {
            navigator.openHomeScreen()
        } else navigator.goBack()
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    bottom = 20.dp,
                    end = 20.dp,
                    start = 20.dp
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.flixclusive_tag),
                contentDescription = stringResource(LocaleR.string.flixclusive_tag_content_desc),
                contentScale = ContentScale.FillHeight,
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .height(150.dp)
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(brushGradient, blendMode = BlendMode.SrcAtop)
                        }
                    }
            )

            Text(
                text = stringResource(id = LocaleR.string.update_out_now_format, args.newVersion),
                modifier = Modifier.padding(bottom = 10.dp),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 25.sp, fontWeight = FontWeight.Bold
                )
            )

            MarkdownText(
                markdown = args.updateInfo?.fromGitmoji() ?: stringResource(id = LocaleR.string.default_update_info_message),
                isTextSelectable = true,
                linkColor = Color(0xFF5890FF),
                linkifyMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = LocalContentColor.current,
                ),
                imageLoader = LocalContext.current.imageLoader,
                onLinkClicked = uriHandler::openUri,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(50.dp))
        }

        AnimatedContent(
            label = "",
            targetState = downloadProgress,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(),
                    initialContentExit = fadeOut()
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) { state ->
            when (state == null) {
                true -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .height(70.dp)
                    ) {
                        Button(
                            onClick = {
                                if (readyToInstall) {
                                    context.startActivity(
                                        installApkActivity(apkUri!!)
                                    )
                                    navigator.openHomeScreen()
                                } else context.startAppUpdater(args.updateUrl)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(0.5F)
                                .heightIn(min = 70.dp)
                                .padding(buttonPaddingValues)
                        ) {
                            Text(
                                text = stringResource(LocaleR.string.update_label),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 16.sp
                                ),
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Button(
                            onClick = {
                                if (args.isComingFromSplashScreen) {
                                    navigator.openHomeScreen()
                                } else navigator.goBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = Color.White.onMediumEmphasis()
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(0.5F)
                                .heightIn(min = 70.dp)
                                .padding(buttonPaddingValues)
                        ) {
                            Text(
                                text = stringResource(LocaleR.string.not_now_label),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 16.sp
                                ),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
                false -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .height(70.dp)
                    ) {
                        var labelToUse = stringResource(LocaleR.string.update_label)

                        if (readyToInstall) {
                            labelToUse = stringResource(LocaleR.string.install)
                        } else if (downloadProgress != null && downloadProgress!! > -1) {
                            labelToUse = "$downloadProgress%"
                        }

                        Box(
                            modifier = Modifier
                                .padding(buttonPaddingValues),
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .clip(MaterialTheme.shapes.medium)
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable(readyToInstall) {
                                        if (readyToInstall) {
                                            val installIntent = installApkActivity(apkUri!!)
                                            context.startActivity(installIntent)
                                        }
                                    }
                                    .drawWithContent {
                                        with(drawContext.canvas.nativeCanvas) {
                                            val checkPoint = saveLayer(null, null)

                                            drawContent()
                                            drawRect(
                                                color = progressColor,
                                                size = Size(size.width * progressState, size.height),
                                                blendMode = BlendMode.SrcOut
                                            )
                                            restoreToCount(checkPoint)
                                        }
                                    }
                            ) {
                                Text(
                                    text = labelToUse,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun UpdateScreenPreview() {
    FlixclusiveTheme {
        Surface {
            UpdateScreen(
                navigator = object: StartHomeScreenAction {
                    override fun openHomeScreen() {
                        // Do nothing
                    }

                    override fun goBack() {
                        // Do nothing
                    }
                },
                args = UpdateScreenNavArgs(
                    newVersion = "v1.5.0",
                    updateUrl = "https://www.google.com",
                    updateInfo = "## This is an update message" +
                            "\n\nhahhahaha **HAHAHA PERO**",
                    isComingFromSplashScreen = false
                )
            )
        }
    }
}