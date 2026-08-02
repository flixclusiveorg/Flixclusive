package com.flixclusive.core.presentation.mobile.util

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Same pattern as `feature/mobile/onboarding`'s `PermissionUtil.isStorageAccessGranted`/
 * `createManageStorageIntent`: an `ON_RESUME` recheck plus a dedicated Settings launcher, not a
 * one-shot request. Replicated here (rather than reused) since `feature` modules can't depend on
 * other `feature` modules, and this needs to be reachable from more than one feature.
 */
object BatteryOptimizationUtil {
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun createRequestIgnoreBatteryOptimizationsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }

    @Composable
    fun rememberIsIgnoringBatteryOptimizations(): Pair<Boolean, () -> Unit> {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        var isIgnoringBatteryOptimizations by remember {
            mutableStateOf(isIgnoringBatteryOptimizations(context))
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        return isIgnoringBatteryOptimizations to {
            launcher.launch(createRequestIgnoreBatteryOptimizationsIntent(context))
        }
    }
}
