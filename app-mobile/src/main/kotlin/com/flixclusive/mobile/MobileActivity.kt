package com.flixclusive.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class MobileActivity : ComponentActivity() {
    private val viewModel: MobileAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        installSplashScreen()

        setContent {
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

    override fun onDestroy() {
        // Make sure to properly destroy the WebView to prevent memory leaks
        viewModel.webViewDriver.value?.destroy()
        viewModel.onReleasePlayerCache()
        super.onDestroy()
    }
}

