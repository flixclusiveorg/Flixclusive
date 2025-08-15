package com.flixclusive.core.datastore.util

import androidx.datastore.core.Serializer
import androidx.datastore.core.okio.OkioSerializer
import okio.BufferedSink
import okio.BufferedSource

/**
 * DataStore's original implementation
 * */
internal class OkioSerializerWrapper<T>(private val delegate: Serializer<T>) : OkioSerializer<T> {
    override val defaultValue: T
        get() = delegate.defaultValue

    override suspend fun readFrom(source: BufferedSource): T {
        return delegate.readFrom(source.inputStream())
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        delegate.writeTo(t, sink.outputStream())
    }
}
