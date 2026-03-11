package com.flixclusive.feature.mobile.player.component.server

import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.presentation.common.components.GradientLinearProgressIndicator
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.feature.mobile.player.component.effect.toggleSystemBars
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ProviderLoadingDialog(
    state: LoadLinksState,
    canSkipLoading: Boolean,
    onSkipLoading: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val activity = LocalContext.current.getActivity<ComponentActivity>()
        DisposableEffect(Unit) {
            onDispose {
                activity.toggleSystemBars(isVisible = false)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            AnimatedContent(
                targetState = state,
                label = "",
                transitionSpec = {
                    if (targetState > initialState) {
                        fadeIn() + slideInHorizontally { it } togetherWith
                            fadeOut() + slideOutHorizontally { -it }
                    } else {
                        fadeIn() + slideInHorizontally { -it } togetherWith
                            fadeOut() + slideOutHorizontally { it }
                    }.using(SizeTransform(clip = false))
                },
            ) {
                Text(
                    text = it.message.asString().trim(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        color = Color.White,
                    ),
                )
            }

            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                GradientLinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(0.3f),
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                )
            }

            if (canSkipLoading) {
                TextButton(
                    onClick = onSkipLoading,
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(30.dp),
                ) {
                    Text(
                        text = stringResource(id = LocaleR.string.skip_loading_message),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(alpha = 0.7f),
                        ),
                    )
                }
            }
        }
    }
}
