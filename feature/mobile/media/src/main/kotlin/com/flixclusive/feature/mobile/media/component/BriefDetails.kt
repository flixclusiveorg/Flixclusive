package com.flixclusive.feature.mobile.media.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.MediaDetailsFormatterUtil.formatAsRating
import com.flixclusive.core.presentation.common.util.MediaDetailsFormatterUtil.formatAsRuntime
import com.flixclusive.core.presentation.common.util.MediaDetailsFormatterUtil.formatReleaseDate
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.components.media.GenreButton
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.media.R
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.Genre
import com.flixclusive.model.provider.ProviderMetadata
import java.util.Locale
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BriefDetails(
    onGenreClick: (Genre) -> Unit,
    metadata: MediaMetadata,
    provider: ProviderMetadata?,
    onProviderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val briefDetails = remember(metadata) {
        getBriefDetails(
            context = context,
            media = metadata,
        )
    }

    val noEmphasisContentColor = LocalContentColor.current.copy(0.6f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        ProviderUsed(
            provider = provider,
            onClick = onProviderClick,
        )

        Text(
            text = metadata.title,
            style = MaterialTheme.typography.headlineLarge.asAdaptiveTextStyle(),
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

                val boxModifier = if (isRating) {
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(0.6f),
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                } else {
                    Modifier
                }

                val textModifier = if (isRating) {
                    Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                } else {
                    Modifier
                }

                Box(
                    modifier = boxModifier.align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
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

@Composable
private fun ProviderUsed(
    provider: ProviderMetadata?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val providerName = provider?.name ?: return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick() }
            .padding(3.dp),
    ) {
        ImageWithSmallPlaceholder(
            model = context.buildImageRequest(provider.iconUrl),
            placeholder = painterResource(UiCommonR.drawable.movie_icon),
            contentDescription = provider.name,
            placeholderSize = 12.dp,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .height(20.dp)
                .aspectRatio(1f),
        )

        Text(
            text = providerName,
            style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle().let {
                it.copy(letterSpacing = it.letterSpacing * 1.2f)
            },
            fontWeight = FontWeight.Medium,
            color = LocalContentColor.current.copy(0.7f),
            modifier = Modifier
                .padding(start = 4.dp),
        )
    }
}

@Immutable
private data class ImportantInfo(
    val rating: String?,
    val adult: String?,
    val runtime: String?,
    val language: String?,
    val releaseDate: String?,
    val seasons: String?,
    val episodes: String?,
    val certification: String?
) {
    val asList by lazy {
        listOfNotNull(rating, adult, runtime, language, releaseDate, certification, seasons, episodes)
    }
}

private fun getBriefDetails(
    context: Context,
    media: MediaMetadata,
): ImportantInfo {
    val language = media.language?.let {
        val locale = Locale.Builder().setLanguage(it).build()
        if (locale.language != "und") locale.displayLanguage else null
    }

    val seasons = if (media is Show) {
        context.resources.getQuantityString(
            LocaleR.plurals.season_runtime,
            media.totalSeasons,
            media.totalSeasons,
        )
    } else {
        null
    }

    val episodes = if (media is Show) {
        context.resources.getQuantityString(
            LocaleR.plurals.episode_runtime,
            media.totalEpisodes,
            media.totalEpisodes,
        )
    } else {
        null
    }

    val adult = if (media.adult) context.getString(R.string.adult) else null

    return ImportantInfo(
        rating = media.rating?.formatAsRating()?.asString(context),
        runtime = media.runtime?.formatAsRuntime()?.asString(context),
        releaseDate = media.formatReleaseDate(),
        certification = media.certification,
        adult = adult,
        language = language,
        seasons = seasons,
        episodes = episodes,
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
                provider = DummyDataForPreview.getProviderMetadata(),
                onGenreClick = {},
                onProviderClick = {},
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
