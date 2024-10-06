package com.flixclusive.feature.mobile.player.controls.dialogs.provider

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.locale.R
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientLinearProgressIndicator
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.feature.mobile.player.controls.common.BasePopupScreen
import com.flixclusive.feature.mobile.player.controls.common.EnlargedTouchableButton
import com.flixclusive.model.provider.link.Stream

@Composable
internal fun ProviderResourceStateScreen(
    modifier: Modifier = Modifier,
    state: MediaLinkResourceState,
    servers: List<Stream>,
    onSkipLoading: () -> Unit,
    onClose: () -> Unit,
) {
    val canSkipLoading by remember {
        derivedStateOf {
            state.isLoading && servers.isNotEmpty()
        }
    }

    LaunchedEffect(key1 = state) {
        if (state is MediaLinkResourceState.Success) {
            onClose()
        }
    }

    BasePopupScreen(
        modifier = modifier,
        onDismiss = onClose,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, top = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                EnlargedTouchableButton(
                    iconId = com.flixclusive.core.ui.common.R.drawable.round_close_24,
                    contentDescription = stringResource(id = R.string.close_label),
                    size = 45.dp,
                    onClick = onClose
                )
            }

            ProgressHeader(
                state = state,
                canSkipLoading = canSkipLoading,
                onSkipLoading = onSkipLoading,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun ProgressHeader(
    modifier: Modifier = Modifier,
    state: MediaLinkResourceState,
    canSkipLoading: Boolean,
    onSkipLoading: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = state,
            label = "",
            transitionSpec = {
                if (targetState > initialState) {
                    fadeIn() + slideInHorizontally { it } togetherWith
                            fadeOut() + slideOutHorizontally { -it }
                } else {
                    fadeIn() + slideInHorizontally { -it } + fadeIn() togetherWith
                            fadeOut() + slideOutHorizontally { it }
                }.using(
                    SizeTransform(clip = false)
                )
            },
        ) {
            Text(
                text = it.message.asString().trim(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp
                )
            )
        }

        AnimatedVisibility(
            visible = state.isLoading,
            enter = fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            GradientLinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.3F),
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                )
            )
        }

        if (canSkipLoading) {
            TextButton(
                onClick = onSkipLoading,
                shape = MaterialTheme.shapes.extraSmall,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .height(30.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.skip_loading_message),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ProviderResourceStateScreenPreview() {
    FlixclusiveTheme {
        Surface {
            ProviderResourceStateScreen(
                state = MediaLinkResourceState.Fetching(),
                onClose = {},
                servers = emptyList(),
                onSkipLoading = {}
            )
        }
    }
}