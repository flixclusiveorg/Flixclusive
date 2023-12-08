package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles

import androidx.compose.foundation.Image
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
import com.flixclusive.R
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettings.Companion.CaptionEdgeTypePreference
import com.flixclusive.domain.preferences.AppSettings.Companion.CaptionSizePreference.Companion.getDp
import com.flixclusive.domain.preferences.AppSettings.Companion.CaptionStylePreference.Companion.getTextStyle
import com.flixclusive.presentation.utils.ComposeUtils.BorderedText

@Composable
fun SubtitlePreview(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    shape: Shape
) {
    if(appSettings.isSubtitleEnabled) {
        Box(
            modifier = modifier
                .height(90.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.sample_movie_subtitle_preview),
                contentDescription = "Sample Movie for Subtitle Preview",
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
                when(appSettings.subtitleEdgeType) {
                    CaptionEdgeTypePreference.Drop_Shadow -> {
                        Text(
                            text = stringResource(id = R.string.sample_subtitle_text),
                            style = appSettings.subtitleFontStyle.getTextStyle().copy(
                                color = Color(appSettings.subtitleColor),
                                fontSize = appSettings.subtitleSize.getDp().sp,
                                shadow = Shadow(
                                    offset = Offset(6F, 6F),
                                    blurRadius = 3f,
                                    color = Color(appSettings.subtitleEdgeType.color),
                                ),
                                background = Color(appSettings.subtitleBackgroundColor),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    CaptionEdgeTypePreference.Outline -> {
                        BorderedText(
                            text = stringResource(id = R.string.sample_subtitle_text),
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