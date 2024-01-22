package com.flixclusive.core.ui.common.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun String.createTextFieldValue(): TextFieldValue {
    return TextFieldValue(
        text = this,
        selection = TextRange(length)
    )
}