package com.flixclusive.model.datastore

import com.flixclusive.gradle.entities.Repository
import com.flixclusive.model.datastore.provider.ProviderPreference
import com.flixclusive.model.datastore.provider.util.RepositorySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream


/**
 *
 * A sub data class for provider settings of
 * the main [AppSettings] data class.
 * */
@Serializable
data class AppSettingsProvider(
    val repositories: List<@Serializable(with = RepositorySerializer::class) Repository> = emptyList(),
    val providers: List<ProviderPreference> = emptyList(),
) {
    companion object {
        fun parseFrom(input: InputStream): AppSettingsProvider {
            return Json.decodeFromString(
                deserializer = serializer(),
                string = input.readBytes().decodeToString()
            )
        }
    }
}