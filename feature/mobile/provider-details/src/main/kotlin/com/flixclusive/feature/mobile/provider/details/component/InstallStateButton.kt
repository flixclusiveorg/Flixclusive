package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.flixclusive.feature.mobile.provider.details.InstallState
import com.flixclusive.feature.mobile.provider.details.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private val ButtonSize = 46.dp

private sealed class StableInstallState {
    data object NotInstalled : StableInstallState()
    data object Installing : StableInstallState()
    data object Installed : StableInstallState()
    data object Loading : StableInstallState()
    data object Uninstalling : StableInstallState()
    data class Outdated(val version: String) : StableInstallState()
}

@Composable
internal fun InstallStateButton(
    installState: () -> InstallState,
    onToggleInstallState: () -> Unit,
    onUninstall: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isButtonEnabled by remember {
        mutableStateOf(
            when (installState()) {
                is InstallState.Installing,
                is InstallState.Loading,
                is InstallState.Uninstalling -> false

                else -> true
            }
        )
    }

    val isInstalled by remember {
        derivedStateOf {
            installState() is InstallState.Installed
        }
    }

    val isOutdated by remember {
        derivedStateOf {
            installState() is InstallState.Outdated
        }
    }

    val isDownloading by remember {
        derivedStateOf {
            installState() is InstallState.Installing
        }
    }

    val isStableState by remember {
        derivedStateOf {
            when (val state = installState()) {
                is InstallState.NotInstalled -> StableInstallState.NotInstalled
                is InstallState.Installed -> StableInstallState.Installed
                is InstallState.Loading -> StableInstallState.Loading
                is InstallState.Uninstalling -> StableInstallState.Uninstalling
                is InstallState.Outdated -> StableInstallState.Outdated(version = state.newVersion)
                is InstallState.Installing -> StableInstallState.Installing
            }
        }
    }

    val containerColor by animateColorAsState(
        targetValue = if (isInstalled) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 300)
    )

    val contentColor by animateColorAsState(
        targetValue = if (isInstalled) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(true) {
        snapshotFlow {
            when (installState()) {
                is InstallState.Installing,
                is InstallState.Loading,
                is InstallState.Uninstalling -> false

                else -> true
            }
        }.distinctUntilChanged()
            .collect { enabled ->
                if (!isButtonEnabled && enabled) {
                    // Add small delay to prevent quick toggling of button state during transitions
                    delay(300)
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
            isDownloading,
            enter = fadeIn() + slideInVertically { -it / 4 },
            exit = slideOutVertically { -it / 4 } + fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            DownloadProgressIndicator(
                installState = installState,
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
                enabled = isButtonEnabled,
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
                    modifier = Modifier.fillMaxWidth(),
                    targetState = isStableState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { state ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                    ) {
                        if (
                            state is StableInstallState.NotInstalled
                            || state is StableInstallState.Outdated
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
                                contentDescription = stringResource(R.string.label_uninstall),
                                tint = LocalContentColor.current,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            color = LocalContentColor.current,
                            text = when (state) {
                                is StableInstallState.Loading -> stringResource(R.string.label_loading)
                                is StableInstallState.NotInstalled -> stringResource(R.string.label_install)
                                is StableInstallState.Installed -> stringResource(R.string.label_uninstall)
                                is StableInstallState.Uninstalling -> stringResource(R.string.label_uninstalling)
                                is StableInstallState.Installing -> stringResource(R.string.label_downloading)
                                is StableInstallState.Outdated -> stringResource(R.string.label_update, state.version)
                            }
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
                        enabled = isButtonEnabled,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .height(ButtonSize)
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.delete_outlined),
                            contentDescription = stringResource(LocaleR.string.uninstall),
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
                        enabled = isButtonEnabled,
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
    installState: () -> InstallState,
    modifier: Modifier = Modifier
) {
    val downloadProgress by remember {
        derivedStateOf {
            when (val state = installState()) {
                is InstallState.Installing -> state.progress
                else -> null
            }
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
                text = stringResource(R.string.label_downloading_provider),
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.8f)
            )

            Text(
                text = "${downloadProgress ?: 0}%",
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.8f)
            )
        }

        downloadProgress?.let {
            LinearProgressIndicator(
                progress = { it / 100f },
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Preview
@Composable
private fun InstallStateButtonPreview() {
    val scope = rememberCoroutineScope()
    var installState by remember { mutableStateOf<InstallState>(InstallState.Outdated("2.0.2", null)) }

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
                .fillMaxWidth()
        ) {
            InstallStateButton(
                installState = { installState },
                onUninstall = {},
                onConfigure = {},
                onToggleInstallState = {
                    scope.launch {
                        // Add delay to simulate processing time
                        if (installState is InstallState.NotInstalled) {
                            installState = InstallState.Installing(progress = 0f)
                            delay(2000)
                            installState = InstallState.Installed
                        } else if (installState is InstallState.Installed) {
                            installState = InstallState.Uninstalling
                            delay(2000)
                            installState = InstallState.NotInstalled
                        } else if (installState is InstallState.Outdated) {
                            installState = InstallState.Installing(progress = 0f)
                            delay(2000)
                            installState = InstallState.Installed
                        } else {
                            // No-op for other states
                        }
                    }
                },
            )
        }
    }
}
