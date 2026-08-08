package com.flixclusive.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composer
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.flixclusive.BuildConfig
import com.flixclusive.R
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.util.android.notify
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR

@AndroidEntryPoint
internal class MobileActivity : ComponentActivity() {
    private val viewModel: MobileAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)
        }

        observeProviderUpdateInfo()
        resumeInterruptedDownloads()
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
        viewModel.hideWebViewDriver()
        viewModel.onReleasePlayerCache()
        super.onDestroy()
    }

    /** Picks downloads back up after a force close. Gated on STARTED rather than run straight from
     * [onCreate], since the sweep starts a foreground service and Android 12+ only allows that
     * while the app is genuinely in the foreground. */
    private fun resumeInterruptedDownloads() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onResumeInterruptedDownloads()
            }
        }
    }

    private fun observeProviderUpdateInfo() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.providerUpdateInfo
                    .filterNotNull()
                    .collect { updateInfo ->
                        val message = when (updateInfo) {
                            is ProviderUpdateInfo.Outdated -> getString(
                                R.string.outdated_providers,
                                updateInfo.providerNames.joinToString(", ")
                            )

                            is ProviderUpdateInfo.Updated -> getString(
                                R.string.updated_providers,
                                updateInfo.providerNames.joinToString(", ")
                            )
                        }

                        notify(
                            id = (System.currentTimeMillis() / 1000).toInt(),
                            channelId = UpdateProviderUseCase.NOTIFICATION_ID,
                            channelName = UpdateProviderUseCase.NOTIFICATION_NAME,
                            shouldInitializeChannel = true,
                        ) {
                            setContentTitle(getString(R.string.app_name))
                            setContentText(message)
                            setSmallIcon(UiCommonR.drawable.provider_logo)
                            setOnlyAlertOnce(false)
                            setAutoCancel(true)
                            setColorized(true)
                            setSilent(true)
                            setStyle(
                                NotificationCompat
                                    .BigTextStyle()
                                    .bigText(message),
                            )
                        }
                    }
            }
        }
    }
}
