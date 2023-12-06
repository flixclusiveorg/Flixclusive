package com.flixclusive.providers.models.providers.superstream

import android.os.Build
import androidx.annotation.RequiresApi
import com.flixclusive.providers.models.common.MediaInfo
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


internal data class SuperStreamMediaDetailResponse(
    val code: Int? = null,
    val msg: String? = null,
    val data: MediaData? = null,
) {
    data class MediaData(
        val id: Int? = null,
        val title: String? = null,
        val year: Int? = null,
        val released: String? = null,
        @SerializedName("max_season") val maxSeason: Int? = null,
        @SerializedName("max_episode") val maxEpisode: Int? = null,
    )

    fun convertDateFormat(inputDateString: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            convertDateFormatJavaTime(inputDateString)
        } else {
            convertDateFormatSimpleDateFormat(inputDateString)
        }
    }

    private fun convertDateFormatSimpleDateFormat(inputDateString: String): String {
        val inputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        return try {
            val date = inputFormat.parse(inputDateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            throw Exception("Cannot parse release date of show [${this.data?.title}]")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertDateFormatJavaTime(inputDateString: String): String {
        val inputFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
        val outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

        return try {
            val date = LocalDate.parse(inputDateString, inputFormat)
            date.format(outputFormat)
        } catch (e: Exception) {
            throw Exception("Cannot parse release date of show [${this.data?.title}]")
        }
    }

    companion object {
        fun SuperStreamMediaDetailResponse.toMediaInfo(isMovie: Boolean): MediaInfo {
            return MediaInfo(
                id = data?.id.toString(),
                title = data?.title
                    ?: throw NullPointerException("Movie title should not be blank or null!"),
                releaseDate = if (isMovie)
                    convertDateFormat(data.released!!)
                else data.year.toString(),
                seasons = data.maxSeason,
                episodes = data.maxEpisode,
            )
        }
    }
}
