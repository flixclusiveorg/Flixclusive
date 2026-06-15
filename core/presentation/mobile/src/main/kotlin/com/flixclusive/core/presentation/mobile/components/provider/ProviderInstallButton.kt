package com.flixclusive.core.presentation.mobile.components.provider

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private val ButtonSize = 46.dp

private sealed class StableInstallState {
    data object NotInstalled : StableInstallState()

    data object Installing : StableInstallState()

    data object Installed : StableInstallState()

    data object Loading : StableInstallState()

    data object Uninstalling : StableInstallState()

    data class Outdated(
        val version: String
    ) : StableInstallState()
}

@Stable
sealed class ProviderInstallState {
    data object Loading : ProviderInstallState()

    data object NotInstalled : ProviderInstallState()

    data object Installed : ProviderInstallState()

    data object Uninstalling : ProviderInstallState()

    data class Installing(
        val progress: Float,
        val downloadId: String
    ) : ProviderInstallState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Installing) return false

            return progress.toInt() == other.progress.toInt()
        }

        override fun hashCode(): Int {
            return progress.hashCode()
        }
    }

    data class Outdated(
        val newVersion: String,
        val newChangelogs: String?,
    ) : ProviderInstallState()
}

@Composable
fun ProviderInstallButton(
    state: () -> ProviderInstallState,
    onToggleInstallState: () -> Unit,
    onUninstall: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var isButtonEnabled by remember {
        mutableStateOf(
            when (state()) {
                is ProviderInstallState.Loading,
                is ProviderInstallState.Uninstalling -> false

                else -> true
            }
        )
    }

    val isInstalled by remember {
        derivedStateOf {
            state() is ProviderInstallState.Installed
        }
    }

    val isOutdated by remember {
        derivedStateOf {
            state() is ProviderInstallState.Outdated
        }
    }

    val isInstalling by remember {
        derivedStateOf {
            state() is ProviderInstallState.Installing
        }
    }

    val isStableState by remember {
        derivedStateOf {
            when (val deferred = state()) {
                is ProviderInstallState.NotInstalled -> StableInstallState.NotInstalled
                is ProviderInstallState.Installed -> StableInstallState.Installed
                is ProviderInstallState.Loading -> StableInstallState.Loading
                is ProviderInstallState.Uninstalling -> StableInstallState.Uninstalling
                is ProviderInstallState.Outdated -> StableInstallState.Outdated(version = deferred.newVersion)
                is ProviderInstallState.Installing -> StableInstallState.Installing
            }
        }
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isInstalled || isInstalling -> Color.Transparent
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 300)
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isInstalled -> MaterialTheme.colorScheme.error
            isInstalling -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(state) {
        snapshotFlow {
            when (state()) {
                is ProviderInstallState.Loading,
                is ProviderInstallState.Uninstalling -> false

                else -> true
            }
        }.distinctUntilChanged()
            .collect { enabled ->
                if (!isButtonEnabled && enabled) {
                    // Add small delay to prevent quick toggling of button state during transitions
                    delay(300.milliseconds)
                }

                isButtonEnabled = enabled
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        AnimatedVisibility(
            isInstalling,
            enter = fadeIn() + slideInVertically { -it / 4 },
            exit = slideOutVertically { -it / 4 } + fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            DownloadProgressIndicator(
                providerInstallState = state,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Button(
                onClick = onToggleInstallState,
                enabled = isButtonEnabled && enabled,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                border = ButtonDefaults.outlinedButtonBorder().takeIf { isInstalled },
                modifier = Modifier
                    .height(ButtonSize)
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = isStableState,
                    transitionSpec = {
                        slideInHorizontally { -it / 10 } + fadeIn() togetherWith
                            slideOutHorizontally { it / 10 } + fadeOut()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { state ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                    ) {
                        if (
                            state is StableInstallState.NotInstalled ||
                            state is StableInstallState.Outdated
                        ) {
                            Icon(
                                painter = painterResource(UiCommonR.drawable.download),
                                contentDescription = stringResource(LocaleR.string.label_download),
                                tint = LocalContentColor.current,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (state is StableInstallState.Installed) {
                            Icon(
                                painter = painterResource(UiCommonR.drawable.delete_outlined),
                                contentDescription = stringResource(LocaleR.string.label_uninstall),
                                tint = LocalContentColor.current,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            color = LocalContentColor.current,
                            text = when (state) {
                                is StableInstallState.Loading -> stringResource(LocaleR.string.label_loading)

                                is StableInstallState.NotInstalled -> stringResource(LocaleR.string.label_install)

                                is StableInstallState.Installed -> stringResource(LocaleR.string.label_uninstall)

                                is StableInstallState.Uninstalling -> stringResource(LocaleR.string.label_uninstalling)

                                is StableInstallState.Installing -> stringResource(LocaleR.string.label_cancel)

                                is StableInstallState.Outdated -> stringResource(
                                    LocaleR.string.label_update,
                                    state.version
                                )
                            },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isOutdated,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.error.copy(0.8f)
                ) {
                    OutlinedIconButton(
                        onClick = onUninstall,
                        enabled = isButtonEnabled && enabled,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .height(ButtonSize)
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.delete_outlined),
                            contentDescription = stringResource(LocaleR.string.label_uninstall),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isInstalled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(0.6f)
                ) {
                    OutlinedIconButton(
                        onClick = onConfigure,
                        shape = MaterialTheme.shapes.small,
                        enabled = isButtonEnabled && enabled,
                        modifier = Modifier.height(ButtonSize)
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.provider_settings),
                            contentDescription = stringResource(LocaleR.string.provider_settings),
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressIndicator(
    providerInstallState: () -> ProviderInstallState,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val formattedProgress by remember {
        derivedStateOf {
            "%.2f%%".format(progress)
        }
    }

    LaunchedEffect(providerInstallState) {
        snapshotFlow {
            when (val state = providerInstallState()) {
                is ProviderInstallState.Installing -> state.progress
                else -> null
            }
        }.distinctUntilChanged()
            .filterNotNull()
            .collectLatest { value ->
                progress = value
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(LocaleR.string.label_downloading_provider),
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.8f)
            )

            Text(
                text = formattedProgress,
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.8f)
            )
        }

        LinearProgressIndicator(
            progress = { progress / 100f },
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

@Preview
@Composable
private fun ProviderInstallButtonPreview() {
    val scope = rememberCoroutineScope()
    var providerInstallState by remember {
        mutableStateOf<ProviderInstallState>(
            ProviderInstallState.Outdated(
                "2.0.2",
                null
            )
        )
    }

//    LaunchedEffect(true) {
//        delay(800)
//        installState = InstallState.Installing(progress = 0f)
//        while (installState is InstallState.Installing) {
//            delay(100)
//            val currentProgress = (installState as InstallState.Installing).progress
//            installState = if (currentProgress >= 100) {
//                InstallState.Installed
//            } else {
//                InstallState.Installing(progress = currentProgress + 5)
//            }
//        }
//    }

    FlixclusiveTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
        ) {
            ProviderInstallButton(
                state = { providerInstallState },
                onUninstall = {},
                onConfigure = {},
                onToggleInstallState = {
                    scope.launch {
                        // Add delay to simulate processing time
                        if (providerInstallState is ProviderInstallState.NotInstalled) {
                            providerInstallState = ProviderInstallState.Installing(
                                progress = 0f,
                                downloadId = "dummy_download_id"
                            )
                            delay(2000.milliseconds)
                            providerInstallState = ProviderInstallState.Installed
                        } else if (providerInstallState is ProviderInstallState.Installed) {
                            providerInstallState = ProviderInstallState.Uninstalling
                            delay(2000.milliseconds)
                            providerInstallState = ProviderInstallState.NotInstalled
                        } else if (providerInstallState is ProviderInstallState.Outdated) {
                            providerInstallState = ProviderInstallState.Installing(
                                progress = 0f,
                                downloadId = "dummy_download_id"
                            )
                            delay(2000.milliseconds)
                            providerInstallState = ProviderInstallState.Installed
                        } else {
                            // No-op for other states
                        }
                    }
                },
            )
        }
    }
}
