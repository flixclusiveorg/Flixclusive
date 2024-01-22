package com.flixclusive.feature.mobile.splashScreen

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.common.navigation.StartHomeScreenAction
import com.flixclusive.core.ui.common.navigation.UpdateDialogNavigator
import com.flixclusive.core.ui.setup.SetupScreensViewModel
import com.flixclusive.core.util.activity.getDirectorySize
import com.flixclusive.core.util.activity.hasAllPermissionGranted
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.configuration.UpdateStatus
import com.flixclusive.feature.mobile.splashScreen.component.ErrorDialog
import com.flixclusive.feature.mobile.splashScreen.component.PlayerCacheSizeWarning
import com.flixclusive.model.datastore.NO_LIMIT_PLAYER_CACHE_SIZE
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
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
    val viewModel: SetupScreensViewModel = hiltViewModel()

    val cacheSize = remember { safeCall { getDirectorySize(context.cacheDir) } }

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle(UpdateStatus.Fetching)
    val configurationStatus by viewModel.configurationStatus.collectAsStateWithLifecycle(Resource.Loading)

    var areAllPermissionsGranted by remember { mutableStateOf(context.hasAllPermissionGranted()) }
    var isGoodToGo by remember { mutableStateOf(false) }
    var isDoneAnimating by rememberSaveable { mutableStateOf(false) }
    var showLoadingCircle by rememberSaveable { mutableStateOf(false) }

    val image = AnimatedImageVector.animatedVectorResource(id = R.drawable.flixclusive_animated_tag)
    var atEnd by rememberSaveable { mutableStateOf(false) }
    val brushGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 25.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.height(300.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.padding(bottom = 50.dp)
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
                visible = showLoadingCircle,
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
        }
    }

    LaunchedEffect(isDoneAnimating) {
        if(!isDoneAnimating) {
            atEnd = true
            delay(4000) // Wait for animated tag to finish
            showLoadingCircle = true
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

    if(
        cacheSize != null
        && (cacheSize / (1000L * 1000L)) >= 1000L // = 1GB of cache
        && appSettings.shouldNotifyAboutCache
        && appSettings.preferredDiskCacheSize == NO_LIMIT_PLAYER_CACHE_SIZE
    ) {
        PlayerCacheSizeWarning(
            cacheSize = cacheSize,
            onDismiss = { shouldNotifyAboutCache ->
                viewModel.updateSettings(
                    appSettings.copy(shouldNotifyAboutCache = shouldNotifyAboutCache)
                )

                isGoodToGo = true
            }
        )
    } else {
        isGoodToGo = true
    }

    if(areAllPermissionsGranted && isDoneAnimating && isGoodToGo) {
        if (updateStatus == UpdateStatus.Outdated) {
            navigator.openUpdateScreen(
                newVersion = viewModel.newVersion!!,
                updateInfo = viewModel.updateInfo,
                updateUrl = viewModel.updateUrl!!
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
        else if (updateStatus == UpdateStatus.UpToDate || configurationStatus is Resource.Success) {
            navigator.openHomeScreen()
        }
    }
}