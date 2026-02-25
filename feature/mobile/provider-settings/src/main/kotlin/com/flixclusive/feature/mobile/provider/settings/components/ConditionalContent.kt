package com.flixclusive.feature.mobile.provider.settings.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.feature.mobile.provider.settings.R

@Composable
internal fun ConditionalContent(
    fallback: @Composable () -> Unit = {
        EmptyDataMessage(
            emojiHeader = "\uD83E\uDD37",
            title = stringResource(R.string.empty_settings_title),
            description = stringResource(R.string.empty_settings_message),
            modifier = Modifier.fillMaxSize(),
        )
    },
    content: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val measurables = subcompose("content", content)
        val placeable = measurables.map { it.measure(constraints) }

        val hasContent = placeable.any { it.width > 0 || it.height > 0 }

        if (hasContent) {
            layout(
                width = placeable.maxOfOrNull { it.width } ?: 0,
                height = placeable.maxOfOrNull { it.height } ?: 0,
            ) {
                placeable.forEach { it.placeRelative(0, 0) }
            }
        } else {
            val fallbackMeasurables = subcompose("fallback", fallback)
            val fallbackPlaceable = fallbackMeasurables.map { it.measure(constraints) }

            layout(
                width = fallbackPlaceable.maxOfOrNull { it.width } ?: 0,
                height = fallbackPlaceable.maxOfOrNull { it.height } ?: 0,
            ) {
                fallbackPlaceable.forEach { it.placeRelative(0, 0) }
            }
        }
    }
}
