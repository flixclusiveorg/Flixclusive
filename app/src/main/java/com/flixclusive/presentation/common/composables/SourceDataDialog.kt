package com.flixclusive.presentation.common.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.domain.model.SourceDataState
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

@Composable
fun SourceDataDialog(
    state: SourceDataState,
    isTv: Boolean = false,
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
        if (isTv) {
            SourceDataDialogTvContent(state)
        }
        else {
            SourceDataDialogMobileContent(
                state = state,
                canSkipExtractingPhase = canSkipExtractingPhase,
                onSkipExtractingPhase = onSkipExtractingPhase
            )
        }
    }
}

@Preview
@Composable
fun VideoPlayerDialogPreview() {
    val delay = 2500L
    var canSkipExtractingPhase by remember { mutableStateOf(true) }
    val state: SourceDataState by flow {
        emit(SourceDataState.Fetching())
        delay(delay)
        emit(SourceDataState.Extracting())
        delay(delay)
        canSkipExtractingPhase = true
        delay(delay)
        emit(SourceDataState.Error())
        delay(delay)
        emit(SourceDataState.Unavailable())
        delay(delay)
        emit(SourceDataState.Success)
        delay(delay)
        emit(SourceDataState.Idle)
    }.collectAsStateWithLifecycle(initialValue = SourceDataState.Idle)

    FlixclusiveMobileTheme {
        Box(
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize()
        ) {
            SourceDataDialog(
                state = state,
                isTv = false,
                canSkipExtractingPhase = canSkipExtractingPhase,
                onSkipExtractingPhase = {},
                onConsumeDialog = {}
            )
        }
    }
}