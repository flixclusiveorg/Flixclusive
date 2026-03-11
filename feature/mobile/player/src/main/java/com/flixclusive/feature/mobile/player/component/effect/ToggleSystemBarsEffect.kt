package com.flixclusive.feature.mobile.player.component.effect

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.core.presentation.mobile.extensions.toggleSystemBars

@Composable
internal fun ToggleSystemBarsEffect() {
    val context = LocalContext.current.getActivity<Activity>()

    DisposableEffect(LocalLifecycleOwner.current) {
        context.toggleSystemBars(isVisible = false)

        onDispose {
            context.toggleSystemBars(isVisible = true)
        }
    }
}
