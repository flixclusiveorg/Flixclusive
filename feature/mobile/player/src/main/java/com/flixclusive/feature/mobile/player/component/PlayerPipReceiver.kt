package com.flixclusive.feature.mobile.player.component

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.extensions.ACTION_PIP_CONTROL
import com.flixclusive.core.presentation.player.extensions.PLAYER_PIP_EVENT

@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Composable
internal fun PlayerPipReceiver(player: AppPlayer) {
    val context = LocalContext.current

    DisposableEffect(LocalLifecycleOwner.current) {
        val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent,
            ) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && intent.action == ACTION_PIP_CONTROL
                ) {
                    val event = intent.getIntExtra(PLAYER_PIP_EVENT, -1)
                    if (event == -1) return

                    player.handlePiPEvent(event)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                /* receiver = */ broadcastReceiver,
                /* filter = */ IntentFilter(ACTION_PIP_CONTROL),
                /* flags = */ Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                /* receiver = */ broadcastReceiver,
                /* filter = */ IntentFilter(ACTION_PIP_CONTROL)
            )
        }

        onDispose {
            context.unregisterReceiver(broadcastReceiver)
        }
    }
}
