package com.flixclusive.core.common.file

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

fun File.toUri(
    applicationId: String,
    context: Context,
): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, "$applicationId.storage_provider", this)
    } else {
        toUri()
    }
}
