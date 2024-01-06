package com.flixclusive.provider.superstream.util

import android.util.Base64
import com.flixclusive.provider.superstream.util.MD5Utils.md5
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object CipherUtils {
    private const val ALGORITHM = "DESede"
    private const val TRANSFORMATION = "DESede/CBC/PKCS5Padding"

    fun encrypt(str: String, key: String, iv: String): String? {
        return try {
            val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
            val bArr = ByteArray(24)
            val bytes: ByteArray = key.toByteArray()
            var length = if (bytes.size <= 24) bytes.size else 24
            System.arraycopy(bytes, 0, bArr, 0, length)
            while (length < 24) {
                bArr[length] = 0
                length++
            }
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(bArr, ALGORITHM),
                IvParameterSpec(iv.toByteArray())
            )

            Base64.encodeToString(cipher.doFinal(str.toByteArray()), 2)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Useful for deobfuscation
    fun decrypt(str: String, key: String, iv: String): String? {
        return try {
            val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
            val bArr = ByteArray(24)
            val bytes: ByteArray = key.toByteArray()
            var length = if (bytes.size <= 24) bytes.size else 24
            System.arraycopy(bytes, 0, bArr, 0, length)
            while (length < 24) {
                bArr[length] = 0
                length++
            }
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(bArr, ALGORITHM),
                IvParameterSpec(iv.toByteArray())
            )
            val inputStr = Base64.decode(str.toByteArray(), Base64.DEFAULT)
            cipher.doFinal(inputStr).decodeToString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getVerify(str: String?, str2: String, str3: String): String? {
        if (str != null) {
            return md5(md5(str2) + str3 + str)
        }
        return null
    }
}