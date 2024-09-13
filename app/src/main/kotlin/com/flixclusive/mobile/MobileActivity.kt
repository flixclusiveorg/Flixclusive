package com.flixclusive.mobile

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.mobile.LocalIsInPipMode
import com.flixclusive.core.ui.mobile.LocalKeyEventHandlers
import com.flixclusive.core.ui.mobile.util.toggleSystemBars
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class MobileActivity : ComponentActivity() {
    private val viewModel: MobileAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        installSplashScreen()

        setContent {
            CompositionLocalProvider(LocalKeyEventHandlers provides viewModel.keyEventHandlers) {
                CompositionLocalProvider(LocalIsInPipMode provides viewModel.isInPipMode) {
                    FlixclusiveTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MobileApp(viewModel)
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        viewModel.isInPipMode = isInPictureInPictureMode
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && viewModel.uiState.value.isOnPlayerScreen
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            && viewModel.isPiPModeEnabled
        ) {
            // Force pip mode for player
            enterPictureInPictureMode(
                with(PictureInPictureParams.Builder()) {
                    val width = 16
                    val height = 9
                    setAspectRatio(Rational(width, height))
                    build()
                }
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return viewModel.keyEventHandlers.reversed().any { it(keyCode, event) } || super.onKeyDown(
            keyCode,
            event
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // Re-call this if ever a random window/dialog
        // from a another app pops up on the screen while
        // player is running
        if (hasFocus && viewModel.uiState.value.isOnPlayerScreen) {
            toggleSystemBars(isVisible = false)
        }
    }
}

