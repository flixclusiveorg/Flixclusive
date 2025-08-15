@file:Suppress("ktlint:standard:filename")

package com.flixclusive.feature.mobile.settings.screen.player

import android.content.Context
import android.text.format.Formatter
import com.flixclusive.model.datastore.user.DEFAULT_PLAYER_CACHE_SIZE_AMOUNT
import com.flixclusive.model.datastore.user.player.PlayerQuality
import kotlinx.collections.immutable.toImmutableMap
import java.util.Locale
import com.flixclusive.core.strings.R as LocaleR

internal fun getAvailableQualities(context: Context) =
    PlayerQuality.entries
        .associateWith { it.qualityName.asString(context) }
        .toImmutableMap()

internal val languages =
    Locale
        .getAvailableLocales()
        .distinctBy { it.language }
        .associate {
            it.language to "${it.displayLanguage} [${it.language}]"
        }.toImmutableMap()

internal val playerBufferLengths =
    listOf(
        50L,
        60L,
        90L,
        120L,
        150L,
        180L,
        210L,
        240L,
        300L,
        360L,
        420L,
        480L,
        540L,
        600L,
        900L,
        1200L,
        1800L,
    ).associateWith { durationInSeconds ->
        val minutes = durationInSeconds / 60
        val seconds = durationInSeconds % 60
        when {
            minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }.toImmutableMap()

internal fun getAvailableBufferSizes(context: Context) =
    listOf<Long>(
        -1,
        10,
        20,
        30,
        40,
        50,
        60,
        70,
        80,
        90,
        100,
        150,
        200,
        250,
        300,
        350,
        400,
        450,
        500,
    ).associateWith { size ->
        if (size == -1L) {
            context.getString(LocaleR.string.auto_option)
        } else {
            Formatter.formatShortFileSize(
                // context =
                context,
                // sizeBytes =
                size * 1000L * 1000L,
            )
        }
    }.toImmutableMap()

internal fun getAvailableCacheSizes(context: Context) =
    listOf(
        0L,
        10L,
        20L,
        30L,
        40L,
        50L,
        60L,
        70L,
        80L,
        90L,
        100L,
        150L,
        200L,
        250L,
        300L,
        350L,
        400L,
        450L,
        500L,
        1000L,
        2000L,
        -1L,
    ).associateWith { size ->
        when (size) {
            0L -> context.getString(LocaleR.string.none_label)
            -1L -> context.getString(LocaleR.string.no_cache_limit_label)
            else ->
                Formatter.formatShortFileSize(
                    // context =
                    context,
                    // sizeBytes =
                    size * 1000L * 1000L,
                ) +
                    if (size ==
                        DEFAULT_PLAYER_CACHE_SIZE_AMOUNT
                    ) {
                        " " + context.getString(LocaleR.string.default_label)
                    } else {
                        ""
                    }
        }
    }.toImmutableMap()
