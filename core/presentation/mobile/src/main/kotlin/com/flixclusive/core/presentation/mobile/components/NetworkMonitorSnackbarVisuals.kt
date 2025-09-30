package com.flixclusive.core.presentation.mobile.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.R

/**
 * Custom visuals for the network connectivity Snackbar.
 * */
class NetworkMonitorSnackbarVisuals(
    override val message: String,
    val isDisconnected: Boolean,
) : SnackbarVisuals {
    override val actionLabel: String? get() = null
    override val withDismissAction: Boolean get() = true
    override val duration: SnackbarDuration
        get() = if (isDisconnected) {
            SnackbarDuration.Indefinite
        } else {
            SnackbarDuration.Short
        }

    companion object {
        /**
         * A custom SnackbarHost that displays network connectivity status.
         * */
        @Composable
        fun NetworkMonitorSnackbarHost(
            hostState: SnackbarHostState,
            modifier: Modifier = Modifier,
        ) {
            SnackbarHost(
                hostState = hostState,
                modifier = modifier,
            ) { data ->
                val isDisconnected = (data.visuals as? NetworkMonitorSnackbarVisuals)?.isDisconnected ?: false

                val snackbarContainerColor = if (isDisconnected) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.inverseSurface
                }

                val snackbarContentColor = if (isDisconnected) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.inverseOnSurface
                }

                val dismissButtonColor = if (isDisconnected) {
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                } else {
                    IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }

                val (icon, contentDesc) = if (isDisconnected) {
                    painterResource(id = R.drawable.round_wifi_off_24) to stringResource(R.string.wifi_off_content_desc)
                } else {
                    painterResource(id = R.drawable.round_wifi_24) to stringResource(R.string.wifi_on_content_desc)
                }

                Snackbar(
                    containerColor = snackbarContainerColor,
                    dismissAction = {
                        IconButton(
                            onClick = data::dismiss,
                            colors = dismissButtonColor,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.close_network_snackbar_content_desc),
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(
                            bottom = 10.dp,
                            start = 10.dp,
                            end = 10.dp,
                        ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = icon,
                            contentDescription = contentDesc,
                            tint = snackbarContentColor,
                        )

                        Text(
                            text = data.visuals.message,
                            color = snackbarContentColor,
                        )
                    }
                }
            }
        }
    }
}
