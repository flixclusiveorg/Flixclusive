package com.flixclusive.presentation.mobile.screens.splash_screen

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
class SplashMobileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val intentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                when (result.resultCode) {
                    RESULT_CANCELED -> {
                        setResult(RESULT_OK)
                        finish()
                    }

                    else -> throw IllegalStateException("Invalid result returned by Splash Activity")
                }
            }

        setContent {
            FlixclusiveMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashMobileScreen(
                        onStartUpdate = intentLauncher::launch,
                        onExitApplication = {
                            setResult(RESULT_CANCELED)
                            finish()
                        },
                        onStartMainActivity = {
                            setResult(RESULT_OK)
                            finish()
                        },
                    )
                }
            }
        }
    }
}