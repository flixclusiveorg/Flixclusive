package com.flixclusive.presentation.mobile.splash_screen

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
@UnstableApi
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlixclusiveMobileTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashMobileScreen(
                        onExitApplication = {
                            setResult(RESULT_CANCELED)
                            finish()
                        },
                        onStartMainActivity = {
                            setResult(RESULT_OK)
                            finish()
                        }
                    )
                }
            }
        }
    }
}