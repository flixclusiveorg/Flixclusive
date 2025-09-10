package com.flixclusive.feature.mobile.film.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.FilmFormatterUtil.formatAsRating
import com.flixclusive.core.presentation.common.util.FilmFormatterUtil.formatAsRuntime
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.Companion.getAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.TypographySize
import com.flixclusive.core.presentation.mobile.components.film.GenreButton
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.util.extractYear
import java.util.Locale
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BriefDetails(
    onGenreClick: (Genre) -> Unit,
    metadata: FilmMetadata,
    providerUsed: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val briefDetails = remember(metadata) {
        getBriefDetails(
            context = context,
            film = metadata,
            providerUsed = providerUsed
        )
    }

    val noEmphasisContentColor = LocalContentColor.current.copy(0.6f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = metadata.title,
            style = getAdaptiveTextStyle(style = AdaptiveTextStyle.Emphasized(size = TypographySize.Headline)),
            textAlign = TextAlign.Start,
            softWrap = true,
            modifier = Modifier.fillMaxWidth(),
        )

        FlowRow(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            briefDetails.asList.forEachIndexed { i, item ->
                val isRating = item == briefDetails.rating
                val isProvider = item == providerUsed

                val boxModifier = if (isRating) {
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(0.6f),
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                } else if (isProvider) {
                    Modifier.border(
                        width = 1.dp,
                        color = noEmphasisContentColor,
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                } else {
                    Modifier
                }

                val textModifier = if (isRating || isProvider) {
                    val horizontal = if (isProvider) 5.dp else 3.dp
                    Modifier.padding(horizontal = horizontal, vertical = 1.dp)
                } else {
                    Modifier
                }

                Box(
                    modifier = boxModifier.align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = getAdaptiveTextStyle(size = 12.sp),
                        fontWeight = FontWeight.Black,
                        color = when {
                            isRating -> MaterialTheme.colorScheme.onTertiary
                            else -> noEmphasisContentColor
                        },
                        modifier = textModifier,
                    )
                }

                if (i < briefDetails.asList.lastIndex) {
                    DetailsDivider(
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }

        FlowRow(
            verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Top),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(top = 14.dp),
        ) {
            metadata.genres.forEach {
                GenreButton(
                    genre = it,
                    onClick = onGenreClick,
                )
            }
        }
    }
}

@Composable
private fun DetailsDivider(modifier: Modifier = Modifier) {
    VerticalDivider(
        thickness = getAdaptiveDp(1.dp, 2.dp),
        color = LocalContentColor.current.copy(0.6f),
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .height(getAdaptiveDp(10.dp, 2.dp)),
    )
}

@Immutable
private data class ImportantInfo(
    val rating: String,
    val provider: String?,
    val adult: String?,
    val runtime: String?,
    val language: String?,
    val releaseDate: String?,
    val seasons: String?,
    val episodes: String?,
) {
    val asList by lazy {
        listOfNotNull(rating, provider, adult, runtime, language, releaseDate, seasons, episodes)
    }
}

private fun getBriefDetails(
    context: Context,
    film: FilmMetadata,
    providerUsed: String
): ImportantInfo {
    val language = film.language?.let {
        val locale = Locale.Builder().setLanguage(it).build()
        if (locale.language != "und") locale.displayLanguage else null
    }

    val date = if (film is TvShow) {
        film.parsedReleaseDate
    } else {
        film.year?.toString() ?: film.releaseDate?.extractYear()?.toString() ?: film.parsedReleaseDate
    }

    val seasons = if (film is TvShow) {
        context.resources.getQuantityString(
            LocaleR.plurals.season_runtime,
            film.totalSeasons,
            film.totalSeasons,
        )
    } else {
        null
    }

    val episodes = if (film is TvShow) {
        context.resources.getQuantityString(
            LocaleR.plurals.episode_runtime,
            film.totalEpisodes,
            film.totalEpisodes,
        )
    } else {
        null
    }

    val adult = if (film.adult) context.getString(R.string.adult) else null

    return ImportantInfo(
        rating = film.rating?.formatAsRating()?.asString(context) ?: "0.0",
        runtime = film.runtime?.formatAsRuntime()?.asString(context),
        adult = adult,
        language = language,
        releaseDate = date ?: context.getString(LocaleR.string.no_release_date),
        seasons = seasons,
        episodes = episodes,
        provider = providerUsed.takeIf { !it.equals(DEFAULT_FILM_SOURCE_NAME, true) }
    )
}

@Preview
@Composable
private fun BriefDetailsBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            BriefDetails(
                metadata = remember { DummyDataForPreview.getMovie() },
                providerUsed = "Netflix",
                onGenreClick = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun BriefDetailsCompactLandscapePreview() {
    BriefDetailsBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun BriefDetailsMediumPortraitPreview() {
    BriefDetailsBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun BriefDetailsMediumLandscapePreview() {
    BriefDetailsBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun BriefDetailsExtendedPortraitPreview() {
    BriefDetailsBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun BriefDetailsExtendedLandscapePreview() {
    BriefDetailsBasePreview()
}
