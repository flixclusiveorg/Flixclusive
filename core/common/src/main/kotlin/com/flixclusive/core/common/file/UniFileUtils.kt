package com.flixclusive.core.common.file

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile.fromUri

object UniFileUtils {
    fun getFilePath(context: Context, uri: Uri?): String? {
        if (uri == null) return null

        val file = fromUri(context, uri)
        return file?.filePath
    }
}
