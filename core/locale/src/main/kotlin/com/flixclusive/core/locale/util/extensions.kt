package com.flixclusive.core.locale.util

import com.flixclusive.core.locale.UiText

fun String.toUiText(): UiText.StringValue {
    return UiText.StringValue(this)
}

fun Int.toUiText(vararg args: Any): UiText.StringResource {
    return UiText.StringResource(this, *args)
}
