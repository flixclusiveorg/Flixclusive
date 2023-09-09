package com.flixclusive.presentation.mobile.common.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flixclusive.R

val LARGE_ERROR = 480.dp
val SMALL_ERROR = 110.dp

@Composable
fun ErrorScreenWithButton(
    modifier: Modifier = Modifier,
    shouldShowError: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit,
) {
    AnimatedVisibility(
        visible = shouldShowError,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error ?: stringResource(id = R.string.pagination_error_message),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onRetry,
                    shape = ShapeDefaults.Medium
                ) {
                    Text(
                        text = stringResource(R.string.retry),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

}