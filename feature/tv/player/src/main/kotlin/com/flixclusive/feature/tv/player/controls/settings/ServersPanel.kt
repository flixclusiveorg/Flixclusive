package com.flixclusive.feature.tv.player.controls.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.PlayerUiState
import com.flixclusive.core.ui.tv.util.hasPressedRight
import com.flixclusive.feature.tv.player.controls.settings.common.ListContentHolder
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.provider.ProviderApi
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun ServersPanel(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    servers: List<Stream>,
    apis: List<ProviderApi>,
    onProviderChange: (String) -> Unit,
    onServerChange: (Int) -> Unit,
    hidePanel: () -> Unit,
) {
    val selectedProviderIndex = remember(state.selectedProvider) {
        apis.indexOfFirst {
            it.provider.name.equals(state.selectedProvider, true)
        }
    }

    var isFirstItemFullyFocused by remember { mutableStateOf(true) }

    val blackBackgroundGradient = Brush.horizontalGradient(
        0F to Color.Black,
        0.85F to Color.Black.onMediumEmphasis(),
        1F to Color.Black.onMediumEmphasis(0.4F),
    )

    BackHandler {
        hidePanel()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(blackBackgroundGradient),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85F)
        ) {
            ListContentHolder(
                modifier = Modifier
                    .weight(1F)
                    .onPreviewKeyEvent {
                        isFirstItemFullyFocused = false
                        false
                    },
                contentDescription = stringResource(id = LocaleR.string.providers),
                label = stringResource(id = LocaleR.string.providers),
                items = apis,
                selectedIndex = selectedProviderIndex,
                itemState = state.selectedProviderState,
                onItemClick = {
                    onProviderChange(apis[it].provider.name!!)
                }
            )

            ListContentHolder(
                modifier = Modifier
                    .weight(1F)
                    .onKeyEvent {
                        if (hasPressedRight(it) && isFirstItemFullyFocused) {
                            hidePanel()
                            return@onKeyEvent true
                        } else isFirstItemFullyFocused = true

                        false
                    },
                contentDescription = stringResource(id = LocaleR.string.servers),
                label = stringResource(id = LocaleR.string.servers),
                items = servers,
                initializeFocus = true,
                selectedIndex = state.selectedSourceLink,
                onItemClick = onServerChange
            )
        }
    }
}

