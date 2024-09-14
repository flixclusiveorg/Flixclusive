package com.flixclusive.core.ui.mobile.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.flixclusive.core.locale.R as LocaleR

val LARGE_ERROR = 480.dp
val SMALL_ERROR = 110.dp

@Composable
fun RetryButton(
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
                    text = error ?: stringResource(id = LocaleR.string.pagination_error_message),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                )

                Button(
                    onClick = onRetry,
                    shape = ShapeDefaults.Medium
                ) {
                    Text(
                        text = stringResource(LocaleR.string.retry),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

}