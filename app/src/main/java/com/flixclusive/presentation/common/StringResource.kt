package com.flixclusive.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText {
    data class StringValue(val str: String) : UiText()

    class StringResource(
        @StringRes val stringId: Int,
        vararg val args: Any,
    ) : UiText()

    fun asString(context: Context): String {
        return when (this) {
            is StringValue -> str
            is StringResource -> context.getString(stringId, args)
        }
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is StringValue -> str
            is StringResource -> stringResource(id = stringId, formatArgs = args)
        }
    }
}