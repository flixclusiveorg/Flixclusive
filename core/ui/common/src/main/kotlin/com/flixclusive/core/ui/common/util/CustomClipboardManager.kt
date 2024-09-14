package com.flixclusive.core.ui.common.util

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.flixclusive.core.locale.R as LocaleR


class CustomClipboardManager(
    private val clipboard: ClipboardManager,
    private val context: Context
) {
    fun setText(
        text: String,
        toastMessage: String? = null
    ) {
        clipboard.setText(AnnotatedString(text))

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            context.showToast(toastMessage ?: context.getString(LocaleR.string.copied_to_clipboard), Toast.LENGTH_SHORT)
    }

    companion object {
        @Composable
        fun rememberClipboardManager(): CustomClipboardManager {
            val clipboardManager = LocalClipboardManager.current
            val context = LocalContext.current

            return remember {
                CustomClipboardManager(
                    clipboard = clipboardManager,
                    context = context
                )
            }
        }
    }
}