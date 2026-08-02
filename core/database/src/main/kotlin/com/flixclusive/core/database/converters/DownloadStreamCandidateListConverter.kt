package com.flixclusive.core.database.converters

import androidx.room.TypeConverter
import com.flixclusive.core.database.entity.downloads.DownloadStreamCandidate
import org.json.JSONArray
import org.json.JSONObject

internal class DownloadStreamCandidateListConverter {
    @TypeConverter
    fun fromList(value: List<DownloadStreamCandidate>?): String? {
        if (value == null) return null

        val array = JSONArray()
        value.forEach { candidate ->
            val obj = JSONObject().put("url", candidate.url)
            candidate.headers?.let { obj.put("headers", JSONObject(it)) }
            array.put(obj)
        }

        return array.toString()
    }

    @TypeConverter
    fun toList(value: String?): List<DownloadStreamCandidate>? {
        if (value == null) return null

        val array = JSONArray(value)
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val headers = obj.optJSONObject("headers")?.let { headersObj ->
                    buildMap {
                        for (key in headersObj.keys()) {
                            put(key, headersObj.getString(key))
                        }
                    }
                }
                add(DownloadStreamCandidate(url = obj.getString("url"), headers = headers))
            }
        }
    }
}
