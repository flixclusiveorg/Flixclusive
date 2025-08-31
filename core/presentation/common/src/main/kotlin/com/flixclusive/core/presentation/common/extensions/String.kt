package com.flixclusive.core.presentation.common.extensions

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Converts a [String] to a [TextFieldValue] with the cursor positioned at the end of the text.
 *
 * @return A [TextFieldValue] containing the original string and the cursor at the end.
 * */
fun String.toTextFieldValue(): TextFieldValue {
    return TextFieldValue(
        text = this,
        selection = TextRange(length)
    )
}
