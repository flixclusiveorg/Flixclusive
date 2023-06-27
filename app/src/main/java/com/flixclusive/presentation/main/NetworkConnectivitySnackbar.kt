package com.flixclusive.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flixclusive.R

class NetworkConnectivitySnackbarVisuals(
    override val message: String,
    val isDisconnected: Boolean
) : SnackbarVisuals {
    override val actionLabel: String?
        get() = null
    override val duration: SnackbarDuration
        get() = if(isDisconnected) SnackbarDuration.Indefinite else SnackbarDuration.Short
    override val withDismissAction: Boolean
        get() = true
}

@Composable
fun NetworkConnectivitySnackbar(
    hostState: SnackbarHostState
) {
    SnackbarHost(hostState = hostState) { data ->
        val isDisconnected = (data.visuals as? NetworkConnectivitySnackbarVisuals)?.isDisconnected ?: false

        val snackbarContainerColor = if(isDisconnected) {
            MaterialTheme.colorScheme.errorContainer
        } else MaterialTheme.colorScheme.inverseSurface

        val snackbarContentColor = if(isDisconnected) {
            MaterialTheme.colorScheme.error
        } else MaterialTheme.colorScheme.inverseOnSurface

        val dismissButtonColor = if (isDisconnected) {
            IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        } else {
            IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            )
        }

        val snackbarStartDrawable = if(isDisconnected) {
            painterResource(id = R.drawable.round_wifi_off_24)
        } else painterResource(id = R.drawable.round_wifi_24)

        Snackbar(
            containerColor = snackbarContainerColor,
            dismissAction = {
                IconButton(
                    onClick = data::dismiss,
                    colors = dismissButtonColor
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "An icon for the network snackbar close button"
                    )
                }
            },
            modifier = Modifier
                .padding(
                    bottom = 10.dp,
                    start = 10.dp,
                    end = 10.dp
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = snackbarStartDrawable,
                    contentDescription = "Icon of network snackbar message",
                    tint = snackbarContentColor
                )

                Text(
                    text = data.visuals.message,
                    color = snackbarContentColor
                )
            }
        }
    }
}