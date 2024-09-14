package com.flixclusive.feature.mobile.settings.component.dialog.subtitles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.getTextStyle
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.player.CaptionEdgeTypePreference
import com.flixclusive.model.datastore.player.CaptionSizePreference.Companion.getDp
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun SubtitlePreview(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    shape: Shape,
) {
    AnimatedVisibility(visible = appSettings.isSubtitleEnabled) {
        Box(
            modifier = modifier
                .height(90.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = UiCommonR.drawable.sample_movie_subtitle_preview),
                contentDescription = stringResource(LocaleR.string.sample_movie_content_desc),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(shape)
            )

            Box(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                when (appSettings.subtitleEdgeType) {
                    CaptionEdgeTypePreference.Drop_Shadow -> {
                        Box(
                            modifier = Modifier
                                .background(Color(appSettings.subtitleBackgroundColor))
                        ) {
                            Text(
                                text = stringResource(id = LocaleR.string.sample_subtitle_text),
                                style = appSettings.subtitleFontStyle.getTextStyle().copy(
                                    color = Color(appSettings.subtitleColor),
                                    fontSize = appSettings.subtitleSize.getDp().sp,
                                    shadow = Shadow(
                                        offset = Offset(6F, 6F),
                                        blurRadius = 3f,
                                        color = Color(appSettings.subtitleEdgeType.color),
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }

                    CaptionEdgeTypePreference.Outline -> {
                        BorderedText(
                            text = stringResource(id = LocaleR.string.sample_subtitle_text),
                            borderColor = Color(appSettings.subtitleEdgeType.color),
                            style = appSettings.subtitleFontStyle.getTextStyle().copy(
                                color = Color(appSettings.subtitleColor),
                                fontSize = appSettings.subtitleSize.getDp().sp,
                                background = Color(appSettings.subtitleBackgroundColor),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }
}