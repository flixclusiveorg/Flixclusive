package com.flixclusive.feature.splashScreen

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.common.navigation.StartHomeScreenAction
import com.flixclusive.core.ui.common.navigation.UpdateDialogNavigator
import com.flixclusive.core.ui.setup.SetupScreensViewModel
import com.flixclusive.core.util.android.hasAllPermissionGranted
import com.flixclusive.core.util.common.resource.Resource
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
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

interface SplashScreenNavigator : UpdateDialogNavigator, StartHomeScreenAction {
    fun onExitApplication()
}

@OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalPermissionsApi::class)
@Destination
@Composable
fun SplashScreen(
    navigator: SplashScreenNavigator
) {
    val context = LocalContext.current
    val splashScreenViewModel: SplashScreenViewModel = hiltViewModel()
    val setupViewModel: SetupScreensViewModel = hiltViewModel()

    val appSettings by splashScreenViewModel.appSettings.collectAsStateWithLifecycle()
    val uiState by splashScreenViewModel.uiState.collectAsStateWithLifecycle()
    val updateStatus by setupViewModel.updateStatus.collectAsStateWithLifecycle(UpdateStatus.Fetching)
    val configurationStatus by setupViewModel.configurationStatus.collectAsStateWithLifecycle(Resource.Loading)

    var areAllPermissionsGranted by remember { mutableStateOf(context.hasAllPermissionGranted()) }
    var isDoneAnimating by rememberSaveable { mutableStateOf(false) }
    var showLoadingContent by rememberSaveable { mutableStateOf(false) }
    var showDisclaimer by rememberSaveable { mutableStateOf(false) }

    val image = AnimatedImageVector.animatedVectorResource(id = UiCommonR.drawable.flixclusive_animated_tag)
    var atEnd by rememberSaveable { mutableStateOf(false) }

    // Override tv theme if we're on tv
    FlixclusiveTheme {
        val brushGradient = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .heightIn(min = 300.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val paddingBottom by animateDpAsState(
                    targetValue = if (!appSettings.isFirstTimeUserLaunch_) 10.dp else 50.dp,
                    label = ""
                )

                Box(
                    modifier = Modifier.padding(bottom = paddingBottom)
                ) {
                    Image(
                        painter = rememberAnimatedVectorPainter(image, atEnd),
                        contentDescription = stringResource(id = UtilR.string.animated_tag_content_desc),
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

                AnimatedVisibility(
                    visible = showLoadingContent && !appSettings.isFirstTimeUserLaunch_,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    GradientCircularProgressIndicator(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    )
                }

                Box(
                    contentAlignment = Alignment.Center
                ) {
                    this@Column.AnimatedVisibility(
                        visible = showLoadingContent && !showDisclaimer && appSettings.isFirstTimeUserLaunch_,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        PrivacyNotice(
                            nextStep = {
                                splashScreenViewModel.updateSettings(
                                    appSettings.copy(isSendingCrashLogsAutomatically = it)
                                )

                                showDisclaimer = true
                            }
                        )
                    }

                    this@Column.AnimatedVisibility(
                        visible = showDisclaimer && appSettings.isFirstTimeUserLaunch_,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ProvidersDisclaimer(
                            understood = {
                                splashScreenViewModel.updateSettings(
                                    appSettings.copy(isFirstTimeUserLaunch_ = false)
                                )
                            }
                        )
                    }
                }
            }
        }

        LaunchedEffect(isDoneAnimating) {
            if(!isDoneAnimating) {
                atEnd = true
                delay(4000) // Wait for animated tag to finish
                showLoadingContent = true
                isDoneAnimating = true
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationsPermissionState = rememberPermissionState(
                android.Manifest.permission.POST_NOTIFICATIONS
            )

            val textToShow = if (notificationsPermissionState.status.shouldShowRationale) {
                stringResource(UtilR.string.notification_persist_request_message)
            } else {
                stringResource(UtilR.string.notification_request_message)
            }

            if(!notificationsPermissionState.status.isGranted) {
                ErrorDialog(
                    title = stringResource(UtilR.string.splash_notice_permissions_header),
                    description = textToShow,
                    dismissButtonLabel = stringResource(UtilR.string.allow),
                    onDismiss = notificationsPermissionState::launchPermissionRequest
                )
            }

            if(notificationsPermissionState.status.isGranted && !areAllPermissionsGranted) {
                isDoneAnimating = false // Repeat animation.
                areAllPermissionsGranted = true
            }
        }
        else areAllPermissionsGranted = true

        if(areAllPermissionsGranted && isDoneAnimating && !appSettings.isFirstTimeUserLaunch_) {
            if (updateStatus == UpdateStatus.Outdated) {
                navigator.openUpdateScreen(
                    newVersion = setupViewModel.newVersion!!,
                    updateInfo = setupViewModel.updateInfo,
                    updateUrl = setupViewModel.updateUrl!!
                )
            }
            else if (updateStatus is UpdateStatus.Error || updateStatus == UpdateStatus.Maintenance || configurationStatus is Resource.Failure) {
                val (title, description) = if(updateStatus == UpdateStatus.Maintenance) {
                    Pair(
                        stringResource(UtilR.string.splash_maintenance_header),
                        stringResource(UtilR.string.splash_maintenance_message)
                    )
                } else {
                    val errorMessage = if(updateStatus is UpdateStatus.Error)
                        updateStatus.errorMessage
                    else (configurationStatus as Resource.Failure).error

                    Pair(
                        stringResource(UtilR.string.something_went_wrong),
                        errorMessage!!.asString()
                    )
                }

                ErrorDialog(
                    title = title,
                    description = description,
                    onDismiss = navigator::onExitApplication
                )
            }
            else if ((updateStatus == UpdateStatus.UpToDate || configurationStatus is Resource.Success) && uiState is SplashScreenUiState.Okay) {
                navigator.openHomeScreen()
            }
        }
    }
}