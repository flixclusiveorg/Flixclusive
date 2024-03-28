package com.flixclusive.core.datastore.serializer

import androidx.datastore.core.Serializer
import com.flixclusive.model.datastore.AppSettingsProvider
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object AppSettingsProviderSerializer : Serializer<AppSettingsProvider> {
    override val defaultValue: AppSettingsProvider
        get() = AppSettingsProvider()

    override suspend fun readFrom(input: InputStream): AppSettingsProvider {
        return try {
            AppSettingsProvider.parseFrom(input)
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }


    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: AppSettingsProvider, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = AppSettingsProvider.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}