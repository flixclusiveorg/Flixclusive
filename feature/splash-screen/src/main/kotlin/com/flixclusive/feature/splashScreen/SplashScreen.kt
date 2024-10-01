package com.flixclusive.feature.splashScreen

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.common.navigation.navigator.SplashScreenNavigator
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.data.configuration.UpdateStatus
import com.flixclusive.feature.splashScreen.component.ErrorDialog
import com.flixclusive.feature.splashScreen.component.PrivacyNotice
import com.flixclusive.feature.splashScreen.component.ProvidersDisclaimer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR


@OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalPermissionsApi::class)
@Destination
@Composable
internal fun SplashScreen(
    navigator: SplashScreenNavigator
) {
    val context = LocalContext.current
    val viewModel: SplashScreenViewModel = hiltViewModel()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val onBoardingPreferences by viewModel.onBoardingPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateStatus by viewModel.appUpdateCheckerUseCase.updateStatus.collectAsStateWithLifecycle()
    val configurationStatus by viewModel.configurationStatus.collectAsStateWithLifecycle()

    var areAllPermissionsGranted by remember { mutableStateOf(context.hasAllPermissionGranted()) }
    var isDoneAnimating by rememberSaveable { mutableStateOf(false) }
    var showDisclaimer by rememberSaveable { mutableStateOf(false) }

    val localDensity = LocalDensity.current
    var disclaimerHeightDp by remember { mutableStateOf(0.dp) }

    val image =
        AnimatedImageVector.animatedVectorResource(id = UiCommonR.drawable.flixclusive_animated_tag)
    var atEnd by rememberSaveable { mutableStateOf(false) }

    // Override tv theme if we're on tv
    FlixclusiveTheme {
        val brushGradient = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val paddingBottom by animateDpAsState(
                targetValue = if (!onBoardingPreferences.isFirstTimeUserLaunch_) 30.dp else 50.dp,
                label = ""
            )

            Box(
                modifier = Modifier.padding(bottom = paddingBottom)
            ) {
                Image(
                    painter = rememberAnimatedVectorPainter(image, atEnd),
                    contentDescription = stringResource(id = LocaleR.string.animated_tag_content_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(300.dp)
                        .graphicsLayer(alpha = 0.99f)
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                drawRect(brushGradient, blendMode = BlendMode.SrcAtop)
                            }
                        }
                )
            }

            AnimatedContent(
                targetState = onBoardingPreferences.isFirstTimeUserLaunch_,
                contentAlignment = Alignment.TopCenter,
                label = ""
            ) { state ->
                when {
                    state && isDoneAnimating -> {
                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(disclaimerHeightDp)
                        ) {
                            AnimatedContent(
                                targetState = showDisclaimer,
                                contentAlignment = Alignment.TopCenter,
                                label = ""
                            ) { stateDisclaimer ->
                                when(stateDisclaimer) {
                                    false -> {
                                        PrivacyNotice(
                                            nextStep = {
                                                viewModel.updateSettings(
                                                    appSettings.copy(
                                                        isSendingCrashLogsAutomatically = it
                                                    )
                                                )

                                                showDisclaimer = true
                                            },
                                            modifier = Modifier
                                                .onGloballyPositioned {
                                                    val height = with(localDensity) { it.size.height.toDp() }

                                                    disclaimerHeightDp = max(height, disclaimerHeightDp)
                                                }
                                        )
                                    }
                                    true -> {
                                        ProvidersDisclaimer(
                                            understood = { _ ->
                                                viewModel.updateOnBoardingPreferences {
                                                    it.copy(isFirstTimeUserLaunch_ = false)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    isDoneAnimating -> {
                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .ifElse(
                                    condition = disclaimerHeightDp > 0.dp,
                                    ifTrueModifier = Modifier.height(disclaimerHeightDp)
                                )
                        ) {
                            GradientCircularProgressIndicator(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                                modifier = Modifier
                                    .animateEnterExit(
                                        enter = scaleIn(),
                                        exit = scaleOut(),
                                    )
                            )
                        }
                    }
                }
            }
        }

        LaunchedEffect(isDoneAnimating) {
            if (!isDoneAnimating) {
                atEnd = true
                delay(4000) // Wait for animated tag to finish
                isDoneAnimating = true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationsPermissionState = rememberPermissionState(
                android.Manifest.permission.POST_NOTIFICATIONS
            )

            val textToShow = if (notificationsPermissionState.status.shouldShowRationale) {
                stringResource(LocaleR.string.notification_persist_request_message)
            } else {
                stringResource(LocaleR.string.notification_request_message)
            }

            if (!notificationsPermissionState.status.isGranted) {
                ErrorDialog(
                    title = stringResource(LocaleR.string.splash_notice_permissions_header),
                    description = textToShow,
                    dismissButtonLabel = stringResource(LocaleR.string.allow),
                    onDismiss = notificationsPermissionState::launchPermissionRequest
                )
            }

            if (notificationsPermissionState.status.isGranted && !areAllPermissionsGranted) {
                isDoneAnimating = false // Repeat animation.
                areAllPermissionsGranted = true
            }
        } else areAllPermissionsGranted = true

        if (areAllPermissionsGranted && isDoneAnimating && !onBoardingPreferences.isFirstTimeUserLaunch_) {
            if (updateStatus is UpdateStatus.Outdated && appSettings.isUsingAutoUpdateAppFeature) {
                navigator.openUpdateScreen(
                    newVersion = viewModel.appUpdateCheckerUseCase.newVersion!!,
                    updateInfo = viewModel.appUpdateCheckerUseCase.updateInfo,
                    updateUrl = viewModel.appUpdateCheckerUseCase.updateUrl!!,
                    isComingFromSplashScreen = true,
                )
            } else if (
                ((updateStatus is UpdateStatus.Error) && appSettings.isUsingAutoUpdateAppFeature)
                || configurationStatus is Resource.Failure
            ) {
                val (title, errorMessage) = if (updateStatus is UpdateStatus.Error)
                    stringResource(LocaleR.string.failed_to_get_app_updates) to updateStatus.errorMessage
                else stringResource(LocaleR.string.something_went_wrong) to (configurationStatus as Resource.Failure).error

                ErrorDialog(
                    title = title,
                    description = errorMessage!!.asString(),
                    dismissButtonLabel = stringResource(LocaleR.string.close_label),
                    onDismiss = navigator::openHomeScreen
                )
            } else if (
                (((updateStatus is UpdateStatus.UpToDate) && appSettings.isUsingAutoUpdateAppFeature)
                    || configurationStatus is Resource.Success)
                && uiState is SplashScreenUiState.Okay
            ) {
                if (updateStatus is UpdateStatus.Error) {
                    LocalContext.current
                        .showToast(
                            message = """
                                ${stringResource(LocaleR.string.failed_to_get_app_updates)}: ${updateStatus.errorMessage?.asString()}
                            """.trimIndent()
                        )
                }

                navigator.openHomeScreen()
            }
        }
    }
}