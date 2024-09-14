package com.flixclusive.core.locale

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
/**
 * A sealed class representing text that can either be a string value or a string resource.
 */
sealed class UiText {
    /**
     * Represents a string value.
     * @param str The string value.
     */
    data class StringValue(val str: String) : UiText() {
        constructor(e: Throwable?) : this(e?.message ?: "")
    }

    /**
     * Represents a string resource.
     * @param stringId The string resource ID.
     * @param args Optional arguments to format the string resource.
     */
    class StringResource(
        @StringRes val stringId: Int,
        vararg val args: Any,
    ) : UiText()

    /**
     * Returns the text as a string.
     * @param context The context used to retrieve string resources.
     * @return The text as a string.
     */
    fun asString(context: Context): String {
        return when (this) {
            is StringValue -> str
            is StringResource -> context.getString(stringId, *args)
        }
    }

    /**
     * Returns the text as a string for Composable functions.
     * @return The text as a string.
     */
    @Composable
    fun asString(): String {
        return when (this) {
            is StringValue -> str
            is StringResource -> stringResource(id = stringId, *args)
        }
    }
}