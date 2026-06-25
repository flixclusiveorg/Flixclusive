package com.flixclusive.core.presentation.common.util

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import com.flixclusive.core.presentation.common.extensions.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.flixclusive.core.strings.R as LocaleR

class CustomClipboardManager(
    private val clipboard: Clipboard,
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun setText(
        text: String,
        toastMessage: String? = null
    ) {
        val clipData = ClipData.newPlainText("plain text", text)
        scope.launch {
            clipboard.setClipEntry(clipEntry = ClipEntry(clipData))

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                context.showToast(
                    toastMessage ?: context.getString(LocaleR.string.copied_to_clipboard),
                    Toast.LENGTH_SHORT
                )
            }
        }
    }

    suspend fun getText(): String? {
        return clipboard
            .getClipEntry()
            ?.clipData
            ?.getItemAt(0)
            ?.text
            ?.toString()
    }

    companion object {
        @Composable
        fun rememberClipboardManager(): CustomClipboardManager {
            val clipboardManager = LocalClipboard.current
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            return remember {
                CustomClipboardManager(
                    clipboard = clipboardManager,
                    context = context,
                    scope = scope
                )
            }
        }
    }
}
