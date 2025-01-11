package com.flixclusive.feature.splashScreen.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import kotlinx.coroutines.delay
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SplashScreenAlertDialog(
    title: String,
    description: String,
    confirmLabel: String = stringResource(LocaleR.string.close_label),
    onConfirm: () -> Unit,
) {
    val buttonMinHeight = 60.dp


}

@Preview
@Composable
private fun AlertDialogPreview() {
    var hasErrors by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(hasErrors) {
        if (!hasErrors) {
            delay(3000L)
            hasErrors = true
        }
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Gray,
        ) {
            AnimatedVisibility(
                visible = hasErrors,
                enter = fadeIn() + scaleIn(tween(1500)),
                exit = scaleOut() + fadeOut(),
            ) {
                SplashScreenAlertDialog(
                    title = "Cache warning!",
                    description = "Sample error",
                ) { hasErrors = false }
            }
        }
    }
}
