package com.flixclusive.data.backup.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupPreference(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val value: PreferenceValue,
) {
    val asStringOrNull: String?
        get() = (value as? StringPreferenceValue)?.value
}

@Serializable
sealed class PreferenceValue

@Serializable
data class StringPreferenceValue(val value: String) : PreferenceValue()
