package com.flixclusive.core.presentation.mobile.components.dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Used mainly in dialogs where the text can be either a String or an AnnotatedString
 * to avoid unnecessary conversions.
 * */
@Composable
fun CharSequenceText(
    text: CharSequence,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    when (text) {
        is String -> Text(
            text = text,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = modifier
        )
        is AnnotatedString -> Text(
            text = text,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = modifier
        )
    }
}
