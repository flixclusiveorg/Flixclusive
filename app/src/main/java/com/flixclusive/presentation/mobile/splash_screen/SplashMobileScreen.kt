package com.flixclusive.presentation.mobile.splash_screen

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.R
import com.flixclusive.presentation.mobile.common.composables.GradientCircularProgressIndicator
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun SplashMobileScreen(
    onExitApplication: () -> Unit,
    onStartMainActivity: () -> Unit
) {
    val viewModel: SplashScreenViewModel = hiltViewModel()
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val image = AnimatedImageVector.animatedVectorResource(id = R.drawable.flixclusive_animated_tag)
    var isDoneAnimating by rememberSaveable { mutableStateOf(false) }
    var atEnd by rememberSaveable { mutableStateOf(false) }
    var showLoadingCircle by rememberSaveable { mutableStateOf(false) }
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
                    contentDescription = "Animated Flixclusive Tag",
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
                GradientCircularProgressIndicator()
            }
        }
    }

    LaunchedEffect(Unit) {
        atEnd = true
        delay(4000)
        showLoadingCircle = true
        isDoneAnimating = true
    }

    LaunchedEffect(uiState, isDoneAnimating) {
        if(uiState.isDoneInitializing && isDoneAnimating) {
            onStartMainActivity()
        }
    }

    if(uiState.isNeedingAnUpdate && !uiState.isDoneInitializing) {
        UpdateDialog(
            onDismiss = viewModel::onConsumeUpdateDialog,
            onUpdate = {
                uriHandler.openUri(uiState.updateUrl)
            }
        )
    }

    if(uiState.isError || uiState.isMaintenance) {
        val (title, description) = if(uiState.isMaintenance) {
            Pair(stringResource(R.string.splash_maintenance_header), stringResource(R.string.splash_maintenance_message))
        } else {
            Pair(stringResource(R.string.splash_error_header), stringResource(R.string.splash_error_message))
        }

        ErrorDialog(
            title = title,
            description = description,
            onDismiss = onExitApplication
        )
    }
}