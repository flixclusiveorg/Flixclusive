package com.flixclusive.feature.mobile.settings.screen.subtitles.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.getTextStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.settings.TweakPaddingHorizontal
import com.flixclusive.model.datastore.user.SubtitlesPreferences
import com.flixclusive.model.datastore.user.player.CaptionEdgeTypePreference
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun SubtitlePreview(
    subtitlePreferencesProvider: () -> SubtitlesPreferences,
    areSubtitlesAvailableProvider: () -> Boolean,
) {
    val shape = MaterialTheme.shapes.medium

    val subtitlesTextStyle =
        with(subtitlePreferencesProvider()) {
            subtitleFontStyle.getTextStyle().copy(
                textAlign = TextAlign.Center,
                color = Color(subtitleColor),
                fontSize = subtitleSize.sp,
            )
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .padding(TweakPaddingHorizontal * 2F)
                .border(
                    width = 2.dp,
                    color = LocalContentColor.current.onMediumEmphasis(),
                    shape = shape,
                ).shadow(
                    elevation = 15.dp,
                    shape = shape,
                ).graphicsLayer {
                    alpha = if (areSubtitlesAvailableProvider()) 1F else 0.4F
                },
    ) {
        Image(
            painter = painterResource(UiCommonR.drawable.sample_movie_subtitle_preview),
            contentDescription = stringResource(LocaleR.string.subtitle_preview_content_desc),
            alignment = Alignment.BottomCenter,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .height(getAdaptiveDp(90.dp)),
        )

        if (subtitlePreferencesProvider().subtitleEdgeType == CaptionEdgeTypePreference.Drop_Shadow) {
            DropShadowTextPreview(
                style = subtitlesTextStyle,
                subtitleBackgroundColor = { subtitlePreferencesProvider().subtitleBackgroundColor },
            )
        } else {
            OutlineTextPreview(
                style = subtitlesTextStyle,
                subtitleBackgroundColor = { subtitlePreferencesProvider().subtitleBackgroundColor },
            )
        }
    }
}

@Composable
internal fun DropShadowTextPreview(
    subtitleBackgroundColor: () -> Int,
    style: TextStyle,
) {
    Box(
        modifier =
            Modifier.drawBehind {
                drawRect(Color(subtitleBackgroundColor()))
            },
    ) {
        Text(
            text = stringResource(LocaleR.string.sample_subtitle_text),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            style =
                style.copy(
                    shadow =
                        Shadow(
                            offset = Offset(6F, 6F),
                            blurRadius = 3f,
                        ),
                ),
        )
    }
}

@Composable
internal fun OutlineTextPreview(
    subtitleBackgroundColor: () -> Int,
    style: TextStyle,
) {
    OutlinedText(
        text = stringResource(LocaleR.string.sample_subtitle_text),
        style = style,
        outlineDrawStyle =
            Stroke(
                miter = 10F,
                width = 10F,
                join = StrokeJoin.Round,
            ),
        modifier =
            Modifier.drawBehind {
                drawRect(Color(subtitleBackgroundColor()))
            },
    )
}

@Preview
@Composable
private fun SubtitlePreviewBasePreview() {
    FlixclusiveTheme {
        Surface {
            SubtitlePreview(
                areSubtitlesAvailableProvider = { false },
                subtitlePreferencesProvider = {
                    SubtitlesPreferences(
                        subtitleEdgeType = CaptionEdgeTypePreference.Outline,
                    )
                }
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun SubtitlePreviewCompactLandscapePreview() {
    SubtitlePreviewBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun SubtitlePreviewMediumPortraitPreview() {
    SubtitlePreviewBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun SubtitlePreviewMediumLandscapePreview() {
    SubtitlePreviewBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun SubtitlePreviewExtendedPortraitPreview() {
    SubtitlePreviewBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun SubtitlePreviewExtendedLandscapePreview() {
    SubtitlePreviewBasePreview()
}
