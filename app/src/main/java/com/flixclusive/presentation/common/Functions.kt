package com.flixclusive.presentation.common

import android.os.Build
import com.flixclusive.common.Constants.FINISH_THRESHOLD
import com.flixclusive.domain.model.entities.WatchHistoryItem
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale

object Functions {
    /**
     * Determines whether the given date string represents a date in the future.
     *
     * @param dateString The date string to check. It should be in the format "yyyy-MM-dd" or "MMMM d, yyyy".
     * @return `true` if the date is in the future, `false` otherwise.
     */
    fun isDateInFuture(dateString: String?): Boolean {
        if(dateString == null || dateString == "No release date" || dateString.isEmpty())
            return true

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val date = try {
                LocalDate.parse(dateString)
            } catch (e: DateTimeParseException) {
                val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
                LocalDate.parse(dateString, formatter)
            }

            date.isAfter(LocalDate.now())
        } else {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val currentDate = Calendar.getInstance().time
            val date = formatter.parse(dateString)
            date?.after(currentDate) ?: false
        }
    }

    /**
     * Determines whether the current playback time is considered as finished watching based on the total duration.
     *
     * @param currentTime The current playback time in milliseconds.
     * @param totalTime The total duration of the media in milliseconds.
     * @return `true` if the current playback time is 95% or more of the total duration, `false` otherwise.
     */
    fun isFinishedWatching(currentTime: Long, totalTime: Long): Boolean {
        val percentage = (currentTime.toDouble() / totalTime.toDouble()) * 100
        return percentage >= FINISH_THRESHOLD
    }

    /**
     * Retrieves the next episode to watch based on the watch history item.
     * If the show has been finished watching, it returns a pair of `null` values to indicate that the show is finished.
     *
     * @param watchHistoryItem The watch history item containing the episodes watched.
     * @return A pair of season number and episode number representing the next episode to watch.
     *         Returns a pair of `null` values if the show has been finished watching.
     */
    fun getNextEpisodeToWatch(watchHistoryItem: WatchHistoryItem): Pair<Int?, Int?> {
        if(watchHistoryItem.episodesWatched.isEmpty())
            return 1 to 1

        val lastEpisodeWatched = watchHistoryItem.episodesWatched.last()
        var seasonNumber = lastEpisodeWatched.seasonNumber!!
        var episodeNumber = lastEpisodeWatched.episodeNumber!!

        if (lastEpisodeWatched.isFinished) {
            val episodeCountMap = watchHistoryItem.episodes
            val isLastEpisodeOfSeason = episodeNumber == getEpisodeCountForSeason(seasonNumber, episodeCountMap)

            if (isLastEpisodeOfSeason) {
                val nextSeasonNumber = seasonNumber + 1

                val isNextSeasonAvailable = episodeCountMap.containsKey(nextSeasonNumber) || nextSeasonNumber < watchHistoryItem.seasons!!
                if (isNextSeasonAvailable) {
                    seasonNumber = nextSeasonNumber
                    episodeNumber = 1
                } else {
                    // The show has been finished watching
                    return null to null
                }
            } else {
                episodeNumber++
            }
        }

        return seasonNumber to episodeNumber
    }

    /**
     * Retrieves the episode count for a given season number.
     *
     * @param seasonNumber The season number.
     * @param episodeCountMap The map containing episode counts for each season.
     * @return The episode count for the given season number.
     *         Returns 0 if the episode count is not available.
     */
    private fun getEpisodeCountForSeason(seasonNumber: Int, episodeCountMap: Map<Int, Int>?): Int = episodeCountMap?.get(seasonNumber) ?: 0

    fun isThereLessThan10SecondsLeftToWatch(currentWatchTime: Long, totalDurationToWatch: Long): Boolean = ((totalDurationToWatch - currentWatchTime) / 1000) <= 10L
}