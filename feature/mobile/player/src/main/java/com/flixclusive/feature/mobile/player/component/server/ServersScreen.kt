package com.flixclusive.feature.mobile.player.component.server

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.noOpClickable
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.feature.mobile.player.component.common.FailedListItemColor
import com.flixclusive.feature.mobile.player.component.common.ListContentHolder
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ServersScreen(
    servers: () -> List<PlayerServer>,
    currentServer: () -> Int,
    onServerChange: (Int) -> Unit,
    providers: List<ProviderMetadata>,
    currentProvider: ProviderMetadata,
    failedStreamUrls: () -> Set<String>,
    onProviderChange: (ProviderMetadata) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboard.current
    val selectedProviderIndex by remember(currentProvider) {
        derivedStateOf { providers.indexOfFirst { it.id == currentProvider.id }.coerceAtLeast(0) }
    }

    val serverList = servers()
    val failedIndices by remember(serverList) {
        derivedStateOf {
            val failedUrls = failedStreamUrls()
            serverList.mapIndexedNotNull { index, server ->
                if (failedUrls.contains(server.url)) index else null
            }.toSet()
        }
    }

    BackHandler {
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.9F))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .noOpClickable()
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(end = 5.dp, top = 10.dp)
                    .align(Alignment.End)
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.round_close_24),
                    contentDescription = stringResource(id = LocaleR.string.close),
                    tint = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight(0.85F)
            ) {
                ListContentHolder(
                    icon = painterResource(id = UiCommonR.drawable.provider_logo),
                    contentDescription = stringResource(id = LocaleR.string.providers),
                    label = stringResource(id = LocaleR.string.providers),
                    items = providers,
                    selectedIndex = selectedProviderIndex,
                    onItemClick = {
                        val provider = providers[it]
                        onProviderChange(provider)
                    },
                    modifier = Modifier
                        .weight(1F)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 10.dp)
                        .fillMaxHeight(0.9F)
                        .width(0.5.dp)
                        .background(LocalContentColor.current.copy(alpha = 0.4F))
                )

                ListContentHolder(
                    icon = painterResource(id = PlayerR.drawable.round_cloud_queue_24),
                    contentDescription = stringResource(id = LocaleR.string.servers),
                    label = stringResource(id = LocaleR.string.servers),
                    items = serverList,
                    selectedIndex = currentServer(),
                    onItemClick = onServerChange,
                    failedIndices = failedIndices,
                    onItemLongClick = {
                        val item = serverList[it]
                        val data = """
                            Server label: ${item.label}
                            Server source: ${item.source}
                            Server URL: ${item.url}
                        """.trimIndent()

                        clipboardManager.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText("${item.label} - ${item.source}", data)
                        )
                    },
                    modifier = Modifier
                        .weight(1F)
                )
            }

            AnimatedVisibility(
                visible = failedIndices.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(FailedListItemColor)
                            .size(8.dp)
                    )

                    Text(
                        text = stringResource(id = PlayerR.string.failed_server_hint),
                        color = Color.White.copy(0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}


