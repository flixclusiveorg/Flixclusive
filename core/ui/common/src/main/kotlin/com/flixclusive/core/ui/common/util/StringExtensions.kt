package com.flixclusive.core.ui.common.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun String.toTextFieldValue(): TextFieldValue {
    return TextFieldValue(
        text = this,
        selection = TextRange(length)
    )
}
