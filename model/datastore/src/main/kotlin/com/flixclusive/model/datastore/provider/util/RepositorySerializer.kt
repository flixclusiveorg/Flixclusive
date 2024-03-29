package com.flixclusive.model.datastore.provider.util

import com.flixclusive.core.util.network.fromJson
import com.flixclusive.gradle.entities.Repository
import com.google.gson.Gson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class RepositorySerializer : KSerializer<Repository> {
    private val gson = Gson()

    private val delegateSerializer = IntArraySerializer()
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("Repository", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: Repository) {
        encoder.encodeString(gson.toJson(value))
    }

    override fun deserialize(decoder: Decoder): Repository {
        return fromJson(decoder.decodeString())
    }
}