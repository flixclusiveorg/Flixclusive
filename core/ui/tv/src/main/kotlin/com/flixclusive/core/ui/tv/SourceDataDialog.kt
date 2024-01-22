package com.flixclusive.core.ui.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
fun SourceDataDialog(
    state: SourceDataState,
    canSkipExtractingPhase: Boolean = false,
    onConsumeDialog: () -> Unit,
    onSkipExtractingPhase: () -> Unit = {},
) {
    LaunchedEffect(key1 = state) {
        if (state is SourceDataState.Success) {
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SourceDataDialogContent(
    state: SourceDataState,
    canSkipExtractingPhase: Boolean = false,
    onSkipExtractingPhase: () -> Unit = {},
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .sizeIn(
                minHeight = 250.dp,
                minWidth = 250.dp
            )
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = state !is SourceDataState.Error && state !is SourceDataState.Unavailable,
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
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }

                    Text(
                        text = state.message.asString(),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            AnimatedVisibility(
                visible = state is SourceDataState.Error || state is SourceDataState.Unavailable,
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
                        contentDescription = stringResource(id = UtilR.string.error_icon_content_desc),
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
            SourceDataDialog(
                state = SourceDataState.Fetching(),
                canSkipExtractingPhase = false,
                onSkipExtractingPhase = {},
                onConsumeDialog = {}
            )
        }
    }
}