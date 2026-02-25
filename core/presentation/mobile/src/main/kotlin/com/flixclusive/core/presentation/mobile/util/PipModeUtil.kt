package com.flixclusive.core.presentation.mobile.util

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.flixclusive.core.presentation.common.extensions.getActivity

object PipModeUtil {
    /**
     * Remembers whether the app is currently in Picture-in-Picture mode.
     *
     * Requires Android Oreo (26) or up. If the device is running a version lower than Oreo,
     * it will always return false.
     *
     * @return True if the app is in PiP mode, false otherwise.
     * */
    @Composable
    fun rememberIsInPipMode(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activity = LocalContext.current.getActivity<ComponentActivity>()
            var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }

            DisposableEffect(activity) {
                val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
                    pipMode = info.isInPictureInPictureMode
                }

                activity.addOnPictureInPictureModeChangedListener(observer)
                onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
            }

            return pipMode
        } else {
            return false
        }
    }
}
