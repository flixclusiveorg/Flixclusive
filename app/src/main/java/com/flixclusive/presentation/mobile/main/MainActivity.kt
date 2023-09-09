package com.flixclusive.presentation.mobile.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.flixclusive.presentation.mobile.splash_screen.SplashActivity
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

val LABEL_START_PADDING = 15.dp
val ITEMS_START_PADDING = 12.dp

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val intentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    mainViewModel.onConfigSuccess()
                }
                RESULT_CANCELED -> {
                    finishAndRemoveTask()
                }
                else -> throw IllegalStateException("Invalid result returned by Splash Activity")
            }
        }

        lifecycleScope.launch {
            mainViewModel.isConfigInitialized
                .combine(mainViewModel.isSplashActivityLaunched) { isConfigInitialized, isSplashActivityLaunched ->
                if(isConfigInitialized) {
                    setContent {
                        FlixclusiveMobileTheme {
                            // A surface container using the 'background' color from the theme
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                MainApp()
                            }
                        }
                    }
                } else if(!isSplashActivityLaunched) {
                    intentLauncher.launch(Intent(this@MainActivity, SplashActivity::class.java))
                    mainViewModel.onSplashActivityLaunch()
                }
            }.collect()
        }
    }
}


