package com.flixclusive.extractor.upcloud.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.flixclusive.core.util.network.CryptographyUtil
import java.io.InputStream

fun getKey(responseBodyStream: InputStream): String {
    val bitmap = BitmapFactory.decodeStream(responseBodyStream)

    val rgba = bitmapToRgba(bitmap)

    val binary = extractBitsFromImage(rgba)
    val hexCompiledString = binaryToASCII(binary)
    val keys = convertHexToIntegers(hexCompiledString)

    return encodeIntegersToBase64(keys)
}

private fun bitmapToRgba(bitmap: Bitmap): ByteArray {
    require(bitmap.config == Bitmap.Config.ARGB_8888) { "Bitmap must be in ARGB_8888 format" }
    val pixels = IntArray(bitmap.width * bitmap.height)
    val bytes = ByteArray(pixels.size * 4)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    var i = 0
    for (pixel in pixels) {
        // Get components assuming is ARGB
        val A = pixel shr 24 and 0xff
        val R = pixel shr 16 and 0xff
        val G = pixel shr 8 and 0xff
        val B = pixel and 0xff
        bytes[i++] = R.toByte()
        bytes[i++] = G.toByte()
        bytes[i++] = B.toByte()
        bytes[i++] = A.toByte()
    }
    return bytes
}

private fun extractBitsFromImage(imageData: ByteArray): String {
    val i = 8 * imageData[3]
    val stringBuilder = StringBuilder()

    for (j in 0 until i) {
        stringBuilder.append(imageData[4 * (j + 1) + 3] % 2)
    }

    return stringBuilder.toString()
}

private fun binaryToASCII(string: String): String {
    val chunks = string.chunked(8) // Splits the string into chunks of 8 characters
    val asciiChars = chunks.map { chunk -> chunk.toInt(2).toChar() } // Converts each chunk from binary to decimal and then to its corresponding ASCII character
    return asciiChars.joinToString("") // Joins the ASCII characters into a single string
}

private fun convertHexToIntegers(hexString: String): List<Int> {
    val keys = mutableListOf<Int>()
    var i = 0
    while (i < hexString.length) {
        // Convert each pair of hexadecimal characters into an integer
        val hexPair = hexString.substring(i, i + 2)
        keys.add(hexPair.toInt(16))
        i += 2
    }
    return keys
}

private fun encodeIntegersToBase64(keys: List<Int>): String {
    val keyBytes = keys.toByteArray()
    return CryptographyUtil.base64Encode(keyBytes, Base64.NO_WRAP) // Encode the bytes to base64
}

private fun List<Int>.toByteArray(): ByteArray {
    val result = ByteArray(size)
    for (i in indices) {
        result[i] = get(i).toByte()
    }
    return result
}