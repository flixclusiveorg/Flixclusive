package com.flixclusive.core.presentation.mobile.util

import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.graphics.toRect
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

    /**
     * Enters Picture-in-Picture mode when the user leaves the app (e.g., presses the home button).
     *
     * Requires Android Oreo (26) or up to Android R (30).
     *
     * @param params The PictureInPictureParams to use when entering PiP mode.
     * */
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun AutoPipModeObserverForAndroidOToR(params: PictureInPictureParams.Builder) {
        val context = LocalContext.current

        DisposableEffect(context) {
            val activity = context.getActivity<ComponentActivity>()

            val onUserLeaveBehavior = Runnable {
                activity.enterPictureInPictureMode(params.build())
            }
            activity.addOnUserLeaveHintListener(onUserLeaveBehavior)
            onDispose {
                activity.removeOnUserLeaveHintListener(onUserLeaveBehavior)
            }
        }
    }

    /**
     * Enters Picture-in-Picture mode when the user leaves the app (e.g., presses the home button).
     *
     * Requires Android S (31) or above.
     *
     * @param context The context to use for entering PiP mode.
     * @param params The PictureInPictureParams to use when entering PiP mode.
     * */
    fun Modifier.pipModeModifierForAndroidSAbove(
        context: Context,
        params: PictureInPictureParams.Builder
    ): Modifier {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onGloballyPositioned { layoutCoordinates ->
                val sourceRect = layoutCoordinates.boundsInWindow().toAndroidRectF().toRect()
                params.setSourceRectHint(sourceRect)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    params.setAutoEnterEnabled(true)
                }

                context.getActivity<ComponentActivity>().setPictureInPictureParams(params.build())
            }
        } else {
            this
        }
    }
}
