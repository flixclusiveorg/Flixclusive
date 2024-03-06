package com.flixclusive.extractor.upcloud.dto

import com.flixclusive.core.util.network.fromJson
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class UpCloudEmbedData(
    val sources: List<DecryptedSource>,
    val tracks: List<UpCloudEmbedSubtitleData>,
    val encrypted: Boolean,
    val server: Int
) {
    data class UpCloudEmbedSubtitleData(
        @SerializedName("file") val url: String,
        @SerializedName("label") val lang: String,
        val kind: String
    )

    companion object {
        fun UpCloudEmbedSubtitleData.toSubtitle() = Subtitle(
            url = url,
            language = lang,
            type = SubtitleSource.ONLINE
        )
    }
}

internal class VidCloudEmbedDataCustomDeserializer(
    private val decryptSource: (String) -> List<DecryptedSource>
): JsonDeserializer<UpCloudEmbedData> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): UpCloudEmbedData {
        val obj = json.asJsonObject
        val tracks = obj.get("tracks").asJsonArray.map {
            fromJson<UpCloudEmbedData.UpCloudEmbedSubtitleData>(it)
        }
        val encrypted = obj.get("t").asInt == 1
        val server = obj.get("server").asInt

        val sources = if (encrypted) {
            decryptSource(obj.get("sources").asString)
        } else {
            obj.get("sources").asJsonArray.map {
                fromJson<DecryptedSource>(it)
            }
        }

        return UpCloudEmbedData(
            sources = sources,
            tracks = tracks,
            encrypted = encrypted,
            server = server,
        )
    }
}