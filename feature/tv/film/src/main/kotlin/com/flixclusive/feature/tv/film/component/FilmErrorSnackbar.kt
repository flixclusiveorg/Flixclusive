@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.feature.tv.film.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun FilmErrorSnackbar(
    errorMessage: UiText?,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = errorMessage,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = slideInVertically(),
                initialContentExit = slideOutVertically()
            )
        },
        label = "",
        modifier = modifier
    ) { message ->
        message?.let {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
                border = Border(
                    BorderStroke(
                        width = 3.dp,
                        color = Color(0xFFC55656)
                    )
                ),
                modifier = Modifier
                    .padding(10.dp)
                    .widthIn(max = 350.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(
                            vertical = 10.dp,
                            horizontal = 20.dp
                        )
                ) {
                    Text(
                        text = stringResource(id = LocaleR.string.something_went_wrong),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        ),
                        color = LocalContentColor.current.onMediumEmphasis(0.8F)
                    )

                    Text(
                        text = it.asString(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        color = LocalContentColor.current.onMediumEmphasis()
                    )
                }
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun FilmErrorSnackbarPreview() {
    var errorMessage by remember { mutableStateOf<UiText?>(null) }

    FlixclusiveTheme(isTv = true) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            colors = NonInteractiveSurfaceDefaults.colors(
                containerColor = Color.White.copy(0.3F)
            )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FilmErrorSnackbar(errorMessage)

                Button(
                    onClick = {
                        errorMessage = if(errorMessage == null) {
                            UiText.StringValue("ERR 404: Failed to fetch the film fetch the film fetch the film fetch the film")
                        } else null
                    }
                ) {
                    Text("Click me")
                }
            }
        }
    }
}