package com.flixclusive.feature.mobile.app.updates.screen

import android.text.util.Linkify
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.imageLoader
import com.flixclusive.core.common.file.extension.toUri
import com.flixclusive.core.common.intent.createApkInstallIntent
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.data.downloads.model.DownloadState
import com.flixclusive.data.downloads.model.DownloadStatus
import com.flixclusive.feature.app.updates.AppUpdatesViewModel
import com.flixclusive.feature.mobile.app.updates.util.fromGitmoji
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Destination<ExternalModuleGraph>
@Composable
internal fun AppUpdatesScreen(
    navigator: NavigatorAppUpdatesScreen,
    newVersion: String,
    updateUrl: String,
    updateInfo: String?,
    isComingFromSplashScreen: Boolean,
    viewModel: AppUpdatesViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    val goBack = fun() {
        val isResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (!isResumed) return

        if (isComingFromSplashScreen) {
            navigator.navigateToHomeScreen()
        } else {
            navigator.navigateBack()
        }
    }

    BackHandler(onBack = goBack)

    AppUpdatesScreenContent(
        applicationId = viewModel.applicationId,
        newVersion = newVersion,
        updateInfo = updateInfo,
        downloadState = { downloadState },
        goBack = goBack,
        downloadUpdate = {
            viewModel.downloadUpdate(
                version = newVersion,
                url = updateUrl,
            )
        },
    )
}

@Composable
private fun AppUpdatesScreenContent(
    applicationId: String,
    newVersion: String,
    updateInfo: String?,
    downloadState: () -> DownloadState,
    downloadUpdate: () -> Unit,
    goBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    val brushGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        ),
    )

    BackHandler {
        goBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(),
        bottomBar = {
            AppUpdatesScreenButtons(
                applicationId = applicationId,
                downloadState = downloadState,
                goBack = goBack,
                downloadUpdate = downloadUpdate,
                modifier = Modifier
                    .navigationBarsPadding()
            )
        },
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
                .padding(it),
        ) {
            Spacer(modifier = Modifier.systemBarsPadding())

            Image(
                painter = painterResource(id = UiCommonR.drawable.flixclusive_tag),
                contentDescription = stringResource(LocaleR.string.flixclusive_tag_content_desc),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth(0.8F)
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(brushGradient, blendMode = BlendMode.SrcAtop)
                        }
                    },
            )

            Text(
                text = stringResource(id = LocaleR.string.update_out_now_format, newVersion),
                modifier = Modifier.padding(bottom = 10.dp),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            MarkdownText(
                markdown = updateInfo?.fromGitmoji() ?: stringResource(id = LocaleR.string.default_update_info_message),
                isTextSelectable = true,
                linkColor = Color(0xFF5890FF),
                linkifyMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
                imageLoader = LocalContext.current.imageLoader,
                onLinkClicked = uriHandler::openUri,
                syntaxHighlightColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = LocalContentColor.current,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.sp,
                ),
            )
        }
    }
}

@Composable
private fun AppUpdatesScreenButtons(
    applicationId: String,
    downloadState: () -> DownloadState,
    goBack: () -> Unit,
    downloadUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val buttonPaddingValues = PaddingValues(horizontal = 5.dp, vertical = 10.dp)
    val progressColor = MaterialTheme.colorScheme.primary

    val status by remember {
        derivedStateOf { downloadState().status }
    }

    fun startInstallation() {
        val uri = downloadState().file!!.toUri(
            applicationId,
            context,
        )

        context.startActivity(createApkInstallIntent(uri))
        goBack()
    }

    AnimatedContent(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .then(modifier),
        targetState = status.isIdle,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(),
                initialContentExit = fadeOut(),
            )
        },
    ) { state ->
        if (state) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
            ) {
                Button(
                    onClick = {
                        if (status.isFinished) {
                            startInstallation()
                        } else {
                            downloadUpdate()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(0.5F)
                        .heightIn(70.dp)
                        .padding(buttonPaddingValues),
                ) {
                    Text(
                        text = stringResource(LocaleR.string.update_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                    )
                }

                Button(
                    onClick = goBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = Color.White.copy(0.6f),
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(0.5F)
                        .heightIn(70.dp)
                        .padding(buttonPaddingValues),
                ) {
                    Text(
                        text = stringResource(LocaleR.string.not_now_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
            ) {
                Box(modifier = Modifier.padding(buttonPaddingValues)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small,
                            )
                            .clip(MaterialTheme.shapes.medium)
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable(status.isFinished) { startInstallation() }
                            .drawWithContent {
                                with(drawContext.canvas.nativeCanvas) {
                                    val checkPoint = saveLayer(null, null)

                                    drawContent()
                                    drawRect(
                                        color = progressColor,
                                        size = Size(size.width * (downloadState().progress / 100), size.height),
                                        blendMode = BlendMode.SrcOut,
                                    )
                                    restoreToCount(checkPoint)
                                }
                            },
                    ) {
                        val label by remember {
                            derivedStateOf {
                                var label = resources.getString(LocaleR.string.update_label)

                                val status = downloadState().status
                                if (status == DownloadStatus.COMPLETED) {
                                    label = resources.getString(LocaleR.string.label_install)
                                } else if (status.isDownloading) {
                                    label = "${String.format(Locale.ROOT, "%.2f", downloadState().progress)}%"
                                }

                                label
                            }
                        }

                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AppUpdatesScreenBasePreview() {
    var state by remember { mutableStateOf(DownloadState.IDLE) }

    LaunchedEffect(state.progress) {
        state = when {
            state.status.isIdle -> DownloadState(
                id = "",
                status = DownloadStatus.DOWNLOADING,
                progress = 0f,
            )

            state.status.isDownloading && state.progress < 100 -> state.copy(
                progress = state.progress + 1,
            )

            state.status.isDownloading && state.progress >= 100 -> DownloadState(
                id = "",
                status = DownloadStatus.COMPLETED,
                progress = 100f,
            )

            else -> DownloadState.IDLE
        }

        delay(300.milliseconds)
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            AppUpdatesScreenContent(
                applicationId = "com.flixclusive",
                newVersion = "1.2.3",
                updateInfo = """
                    ## What's New
                    - 🎉 **New Feature**: Added support for multiple profiles.
                    - 🐛 **Bug Fix**: Fixed crash on startup for some devices.
                    - 🚀 **Performance Improvement**: Optimized image loading times.
                    - 🔒 **Security Update**: Improved encryption for user data.
                    - ✨ **UI Update**: Refreshed the app icon and splash screen.

                    For more details, visit our [website](https://example.com).
                """.trimIndent(),
                downloadState = { state },
                downloadUpdate = {
                    state = state.copy(
                        id = "",
                        status = DownloadStatus.DOWNLOADING,
                        progress = 0f,
                    )
                },
                goBack = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun AppUpdatesScreenCompactLandscapePreview() {
    AppUpdatesScreenBasePreview()
}
