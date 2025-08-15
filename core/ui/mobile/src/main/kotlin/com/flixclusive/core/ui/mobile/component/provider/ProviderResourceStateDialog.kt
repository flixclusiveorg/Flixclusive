package com.flixclusive.core.ui.mobile.component.provider

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.common.provider.MediaLinkResourceState
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
fun ProviderResourceStateDialog(
    state: com.flixclusive.core.common.provider.MediaLinkResourceState,
    canSkipExtractingPhase: Boolean = false,
    onConsumeDialog: () -> Unit,
    onSkipExtractingPhase: () -> Unit = {},
) {
    LaunchedEffect(key1 = state) {
        if (state.isSuccess) {
            onConsumeDialog()
        }
    }

    Dialog(
        onDismissRequest = onConsumeDialog
    ) {
        SourceDataDialogContent(
            state = state,
            canSkipExtractingPhase = canSkipExtractingPhase,
            onSkipExtractingPhase = onSkipExtractingPhase
        )
    }
}

@Composable
private fun SourceDataDialogContent(
    state: com.flixclusive.core.common.provider.MediaLinkResourceState,
    canSkipExtractingPhase: Boolean = false,
    onSkipExtractingPhase: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                this@Column.AnimatedVisibility(
                    visible = !state.isError,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GradientCircularProgressIndicator(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                )
                            )
                        }

                        Text(
                            text = state.message.asString(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                this@Column.AnimatedVisibility(
                    visible = state.isError,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.round_error_outline_24),
                            contentDescription = stringResource(id = LocaleR.string.error_icon_content_desc),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(80.dp)
                                .padding(bottom = 15.dp)
                        )

                        Text(
                            text = state.message.asString(),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = canSkipExtractingPhase,
            enter = fadeIn(),
            exit = fadeOut()
        ){
            Button(onClick = onSkipExtractingPhase) {
                Text(text = stringResource(id = LocaleR.string.skip_loading_message))
            }
        }
    }
}

@Preview
@Composable
fun VideoPlayerDialogPreview() {
//    val delay = 2500L
//    var canSkipExtractingPhase by remember { mutableStateOf(true) }
//    val state: SourceDataState by flow {
//        emit(SourceDataState.Fetching())
//        delay(delay)
//        emit(SourceDataState.Extracting())
//        delay(delay)
//        canSkipExtractingPhase = true
//        delay(delay)
//        emit(SourceDataState.Error())
//        delay(delay)
//        emit(SourceDataState.Unavailable())
//        delay(delay)
//        emit(SourceDataState.Success)
//        delay(delay)
//        emit(SourceDataState.Idle)
//    }.collectAsStateWithLifecycle(initialValue = SourceDataState.Idle)

    FlixclusiveTheme {
        Box(
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize()
        ) {
            ProviderResourceStateDialog(
                state = com.flixclusive.core.common.provider.MediaLinkResourceState.Fetching(),
                canSkipExtractingPhase = false,
                onSkipExtractingPhase = {},
                onConsumeDialog = {}
            )
        }
    }
}
