package com.flixclusive.data.downloads.hls

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-128-CBC decryption for HLS `#EXT-X-KEY:METHOD=AES-128` segments, per RFC 8216 Section 5.2.
 * Uses the JVM's built-in `javax.crypto` (no new dependency).
 */
internal object HlsSegmentDecryptor {
    fun decrypt(
        key: ByteArray,
        iv: ByteArray,
        data: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    /**
     * Parses an HLS IV attribute value into 16 raw bytes. Media3's parser always resolves this to
     * a hex string when a segment is encrypted — either the explicit `0x`-prefixed manifest value,
     * or (when the manifest omits IV) the segment's media sequence number in hex — so this only
     * needs to strip an optional `0x`/`0X` prefix and left-pad to the AES block size.
     */
    fun parseIv(iv: String): ByteArray {
        val hex = iv.removePrefix("0x").removePrefix("0X").padStart(32, '0')
        return ByteArray(16) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
    }
}
