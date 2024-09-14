package com.flixclusive.core.datastore.serializer

import androidx.datastore.core.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

abstract class BaseSettingsSerializer<T>(
    private val serializer: KSerializer<T>
) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T {
        return try {
            return Json.decodeFromString(
                deserializer = serializer,
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = serializer,
                value = t
            ).encodeToByteArray()
        )
    }
}