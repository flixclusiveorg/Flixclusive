package com.flixclusive.presentation.mobile.main

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.util.Rational
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.flixclusive.presentation.mobile.common.KeyEventHandler
import com.flixclusive.presentation.mobile.common.LocalKeyEventHandlers
import com.flixclusive.presentation.mobile.screens.player.utils.toggleSystemBars
import com.flixclusive.presentation.mobile.screens.splash_screen.SplashMobileScreen
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import dagger.hilt.android.AndroidEntryPoint

val LABEL_START_PADDING = 15.dp
val ITEMS_START_PADDING = 12.dp

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainMobileActivityViewModel by viewModels()
    private val sharedViewModel: MainMobileSharedViewModel by viewModels()
    private val keyEventHandlers = mutableListOf<KeyEventHandler>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val intentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                when (result.resultCode) {
                    RESULT_CANCELED -> {
                        setResult(RESULT_OK)
                        mainViewModel.onConfigSuccess()
                    }

                    else -> throw IllegalStateException("Invalid result returned by Splash Activity")
                }
            }

        setContent {
            CompositionLocalProvider(LocalKeyEventHandlers provides keyEventHandlers) {
                FlixclusiveMobileTheme {
                    val isConfigInitialized by mainViewModel.isConfigInitialized.collectAsStateWithLifecycle()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AnimatedContent(
                            targetState = isConfigInitialized,
                            transitionSpec = {
                                ContentTransform(
                                    targetContentEnter = fadeIn(),
                                    initialContentExit = fadeOut()
                                )
                            },
                            label = ""
                        ) { state ->
                            if(state) {
                                MainApp(sharedViewModel)
                            } else {
                                SplashMobileScreen(
                                    onStartUpdate = intentLauncher::launch,
                                    onExitApplication = ::finishAndRemoveTask,
                                    onStartApplication = mainViewModel::onConfigSuccess,
                                )
                            }
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
        sharedViewModel.setPiPModeState(isInPictureInPictureMode)
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onStart() {
        super.onStart()
        mainViewModel.initializeConfigsIfNull()
    }

    override fun onUserLeaveHint() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && sharedViewModel.uiState.value.isInPlayer
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
        return keyEventHandlers.reversed().any { it(keyCode, event) } || super.onKeyDown(keyCode, event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // Failsafe call if ever a random window/dialog
        // from a another app pops up on the screen
        if(hasFocus && sharedViewModel.uiState.value.isInPlayer) {
            toggleSystemBars(isVisible = false)
        }
    }
}

