package com.flixclusive.core.common.file.extension

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile.fromUri
import java.io.File

fun Uri.toFile(context: Context): File? {
    val file = fromUri(context, this)
    return file?.filePath?.let(::File)
}
