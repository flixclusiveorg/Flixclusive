package com.flixclusive.feature.mobile.player.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.feature.mobile.player.R

@Composable
internal fun LockControls(
    unlock: () -> Unit,
    showControls: () -> Unit,
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
                unlock()
                showControls()
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = R.drawable.round_lock_open_24),
                    contentDescription = stringResource(R.string.unlock),
                    tint = Color.White,
                    dp = 40.dp,
                )

                Text(
                    text = stringResource(R.string.unlock),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
