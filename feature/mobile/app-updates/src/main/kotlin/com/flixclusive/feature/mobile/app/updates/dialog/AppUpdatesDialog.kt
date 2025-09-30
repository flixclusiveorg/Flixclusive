package com.flixclusive.feature.mobile.app.updates.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.components.GradientCircularProgressIndicator
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.data.app.updates.model.AppUpdateInfo
import com.flixclusive.feature.app.updates.AppUpdatesUiState
import com.flixclusive.feature.app.updates.AppUpdatesViewModel
import com.flixclusive.feature.mobile.app.updates.R
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.flixclusive.core.strings.R as LocaleR

internal object DismissibleDialog : DestinationStyle.Dialog {
    override val properties =
        DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false,
        )
}

@Destination(style = DismissibleDialog::class)
@Composable
internal fun AppUpdatesDialog(
    navigator: AppUpdatesDialogNavigator,
    viewModel: AppUpdatesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppUpdatesDialogContent(
        uiState = uiState,
        checkForUpdates = viewModel::checkForUpdates,
        openUpdateScreen = { updateInfo ->
            navigator.openUpdateScreen(
                newVersion = updateInfo.versionName,
                updateUrl = updateInfo.updateUrl,
                updateInfo = updateInfo.changelogs,
            )
        },
    )
}

@Composable
private fun AppUpdatesDialogContent(
    uiState: AppUpdatesUiState,
    checkForUpdates: () -> Unit,
    openUpdateScreen: (AppUpdateInfo) -> Unit,
) {
    val rememberedOpenUpdateScreen by rememberUpdatedState(newValue = openUpdateScreen)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Color.Black.copy(0.8f),
                )
                drawCircle(
                    Brush.radialGradient(
                        0.2f to Color.Black.copy(0.6f),
                        1f to Color.Transparent,
                    ),
                )
            },
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        ) {
            AnimatedContent(
                targetState = uiState,
            ) { state ->
                when (state) {
                    is AppUpdatesUiState.Loading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(30.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(getAdaptiveDp(80.dp, 20.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                GradientCircularProgressIndicator(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    ),
                                )
                            }

                            Text(
                                text = stringResource(id = LocaleR.string.checking_for_updates),
                                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                            )
                        }
                    }

                    is AppUpdatesUiState.Error -> {
                        RetryButton(
                            onRetry = checkForUpdates,
                            error = state.message.asString(),
                            modifier = Modifier.padding(bottom = 50.dp),
                        )
                    }

                    is AppUpdatesUiState.UpToDate -> {
                        Column(
                            modifier = Modifier.matchParentSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(30.dp),
                        ) {
                            AdaptiveIcon(
                                painter = painterResource(id = R.drawable.round_check_circle_outline_24),
                                contentDescription = "Updated icon",
                                tint = MaterialTheme.colorScheme.tertiary,
                                dp = 80.dp,
                                modifier = Modifier
                                    .padding(bottom = 15.dp),
                            )

                            Text(
                                text = stringResource(id = LocaleR.string.up_to_date),
                                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    is AppUpdatesUiState.UpdateAvailable -> {
                        LaunchedEffect(true) {
                            val updateInfo = (uiState as AppUpdatesUiState.UpdateAvailable).updateInfo

                            rememberedOpenUpdateScreen(updateInfo)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppUpdatesDialogBasePreview(state: AppUpdatesUiState = AppUpdatesUiState.Loading) {
    FlixclusiveTheme {
        Surface(
            color = MaterialTheme.colorScheme.primary,
        ) {
            AppUpdatesDialogContent(
                uiState = state,
                checkForUpdates = { },
                openUpdateScreen = { _ -> },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun AppUpdatesDialogCompactLandscapePreview() {
    AppUpdatesDialogBasePreview(
        state = AppUpdatesUiState.UpdateAvailable(
            AppUpdateInfo(
                versionName = "1.2.3",
                updateUrl = "https://example.com/update",
                changelogs = "Test",
            ),
        ),
    )
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun AppUpdatesDialogMediumPortraitPreview() {
    AppUpdatesDialogBasePreview(state = AppUpdatesUiState.UpToDate)
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun AppUpdatesDialogMediumLandscapePreview() {
    AppUpdatesDialogBasePreview(AppUpdatesUiState.Error(UiText.from(LocaleR.string.something_went_wrong)))
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun AppUpdatesDialogExtendedPortraitPreview() {
    AppUpdatesDialogBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun AppUpdatesDialogExtendedLandscapePreview() {
    AppUpdatesDialogBasePreview()
}
