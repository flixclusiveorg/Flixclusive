package com.flixclusive.feature.mobile.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.feature.mobile.player.R
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun LockControls(
    areControlsVisible: Boolean,
    shouldLockControls: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    showPlaybackControls: (Boolean) -> Unit,
) {
    AnimatedVisibility(
        visible = areControlsVisible && shouldLockControls,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(Color.Black.copy(0.3F))
                }
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 45.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                onClick = {
                    onVisibilityChange(false)
                    showPlaybackControls(true)
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.round_lock_open_24),
                        contentDescription = stringResource(LocaleR.string.unlock_content_description),
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(bottom = 10.dp)
                    )

                    Text(
                        text = stringResource(LocaleR.string.unlock_label),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}