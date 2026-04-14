package com.flixclusive.feature.mobile.player.component.server

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.presentation.common.components.GradientLinearProgressIndicator
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.core.presentation.mobile.components.GlassSurface
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.extensions.toggleSystemBars
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.drawables.R as UiCommonR
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

        GlassSurface(
            shape = MaterialTheme.shapes.small,
            accentColor = if (state.isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier
                .fillMaxAdaptiveWidth(
                    compact = 0.5f,
                    medium = 0.4f,
                    expanded = 0.35f,
                ),
        ) {
            AnimatedContent(
                targetState = state,
                label = "loading_dialog_content",
                transitionSpec = {
                    fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            ) { currentState ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (currentState.isError) {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.round_error_outline_24),
                            tint = MaterialTheme.colorScheme.error.copy(0.6f),
                            contentDescription = stringResource(id = LocaleR.string.error_icon_content_desc),
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    Text(
                        text = if (currentState.isError) {
                            stringResource(id = LocaleR.string.something_went_wrong)
                        } else {
                            currentState.message.asString().trim()
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = if (currentState.isError) FontWeight.Bold else FontWeight.Normal,
                        ).asAdaptiveTextStyle(14.sp),
                    )

                    if (currentState.isError) {
                        Text(
                            text = currentState.message.asString().trim(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f),
                            ).asAdaptiveTextStyle(12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (currentState.isLoading) {
                        GradientLinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
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
                                style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                            )
                        }
                    }

                    if (currentState.isError) {
                        TextButton(
                            onClick = onDismiss,
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Text(
                                text = stringResource(id = LocaleR.string.close),
                                style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                            )
                        }
                    }
                }
            }
        }
    }
}
