package com.flixclusive.core.util.network

import android.util.Base64
import java.security.DigestException
import java.security.MessageDigest
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptographyUtil {
    /**
     * Exception class representing an error during decryption process.
     * @param message The message describing the decryption error.
     */
    class DecryptionException(message: String) : Exception(message)

    /**
     * Decrypts the specified AES encrypted data using the provided key.
     *
     * @param encryptedData The AES encrypted data to decrypt.
     * @param key The key used for decryption.
     * @return The decrypted data as a string.
     * @throws DecryptionException if an error occurs during decryption process.
     */
    fun decryptAes(encryptedData: String, key: String): String {
        return try {
            val cipherData = Base64.decode(encryptedData, Base64.DEFAULT)
            val saltData: ByteArray = Arrays.copyOfRange(cipherData, 8, 16)

            val md5 = MessageDigest.getInstance("MD5")
            val keyAndIV = generateKeyAndIV(saltData, key.toByteArray(), md5)
                ?: throw Exception("Null Key and IV")

            val keySpec = SecretKeySpec(keyAndIV[0], "AES")
            val iv = IvParameterSpec(keyAndIV[1])

            val encrypted = Arrays.copyOfRange(cipherData, 16, cipherData.size)
            val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")
            aesCBC.init(Cipher.DECRYPT_MODE, keySpec, iv)
            val decryptedData = aesCBC.doFinal(encrypted)

            String(decryptedData)
        } catch (e: Exception) {
            e.printStackTrace()
            throw DecryptionException("Key might be invalid: $key")
        }
    }

    /**
     * Generates a key and an initialization vector (IV) with the given salt and password.
     *
     *
     * This method is equivalent to OpenSSL's EVP_BytesToKey function
     * (see https://github.com/openssl/openssl/blob/master/crypto/evp/evp_key.c).
     * By default, OpenSSL uses a single iteration, MD5 as the algorithm and UTF-8 encoded password data.
     *
     * @param keyLength the length of the generated key (in bytes)
     * @param ivLength the length of the generated IV (in bytes)
     * @param iterations the number of digestion rounds
     * @param salt the salt data (8 bytes of data or `null`)
     * @param password the password data (optional)
     * @param md the message digest algorithm to use
     * @return an two-element array with the generated key and IV
     */
    private fun generateKeyAndIV(
        salt: ByteArray,
        password: ByteArray,
        md: MessageDigest,
        keyLength: Int = 32,
        ivLength: Int = 16,
        iterations: Int = 1,
    ): Array<ByteArray?>? {
        val digestLength = md.digestLength
        val requiredLength = (keyLength + ivLength + digestLength - 1) / digestLength * digestLength
        val generatedData = ByteArray(requiredLength)
        var generatedLength = 0
        return try {
            md.reset()

            // Repeat process until sufficient data has been generated
            while (generatedLength < keyLength + ivLength) {

                // Digest data (last digest if available, password data, salt if available)
                if (generatedLength > 0)
                    md.update(generatedData, generatedLength - digestLength, digestLength)
                md.update(password)
                md.update(salt, 0, 8)
                md.digest(generatedData, generatedLength, digestLength)

                // additional rounds
                for (i in 1 until iterations) {
                    md.update(generatedData, generatedLength, digestLength)
                    md.digest(generatedData, generatedLength, digestLength)
                }
                generatedLength += digestLength
            }

            // Copy key and IV into separate byte arrays
            val result = arrayOfNulls<ByteArray>(2)
            result[0] = generatedData.copyOfRange(0, keyLength)
            if (ivLength > 0) result[1] = generatedData.copyOfRange(keyLength, keyLength + ivLength)
            result
        } catch (e: DigestException) {
            throw RuntimeException(e)
        } finally {
            // Clean out temporary data
            Arrays.fill(generatedData, 0.toByte())
        }
    }

    /**
     * Decodes the specified base64-encoded string.
     * @param data The base64-encoded string to decode.
     * @return The decoded string.
     */
    fun base64Decode(data: String, flag: Int = Base64.DEFAULT): String = String(Base64.decode(data, flag))

    /**
     * Encodes the specified data into a base64-encoded string.
     * @param data The data to encode.
     * @return The base64-encoded string.
     */
    fun base64Encode(data: ByteArray, flag: Int = Base64.DEFAULT): String = String(Base64.encode(data, flag))
}