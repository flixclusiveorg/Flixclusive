package com.flixclusive.providers.sources.superstream.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal object MD5Utils {
    private val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    @JvmOverloads
    fun toHexString(bArr: ByteArray, i: Int = 0, i2: Int = bArr.size): String {
        val cArr = CharArray(i2 * 2)
        var i3 = 0
        for (i4 in i until i + i2) {
            val b = bArr[i4].toInt()
            val i5 = i3 + 1
            val cArr2 = HEX_DIGITS
            cArr[i3] = cArr2[b ushr 4 and 15]
            i3 = i5 + 1
            cArr[i5] = cArr2[b and 15]
        }
        return String(cArr)
    }

    fun md5(hash: String): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(hash.toByteArray())
            toHexString(digest.digest()).lowercase()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        }
    }
}