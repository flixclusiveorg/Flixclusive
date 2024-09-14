package com.flixclusive.core.ui.mobile.component.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.R
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.placeholderEffect
import com.flixclusive.core.locale.R as LocaleR

@Composable
fun FilmCardPlaceholder(
    modifier: Modifier = Modifier,
    title: String? = null,
    isShowingTitle: Boolean = false,
) {
    val contentColor = LocalContentColor.current.onMediumEmphasis()

    val (iconWeight, iconAlignment) = remember {
        if (title != null) {
            0.4F to Alignment.BottomCenter
        } else 1F to Alignment.Center
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .placeholderEffect()
            )

            if (title != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        contentAlignment = iconAlignment,
                        modifier = Modifier.weight(iconWeight)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.movie_icon),
                            contentDescription = stringResource(id = LocaleR.string.film_item_content_description),
                            tint = contentColor,
                            modifier = Modifier
                                .size(40.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .weight(0.6F)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            color = contentColor,
                            modifier = Modifier
                                .padding(8.dp)
                        )
                    }
                }
            }
        }


        if (isShowingTitle) {
            Box(
                modifier = Modifier
                    .padding(
                        vertical = 8.dp,
                        horizontal = 3.dp
                    )
            ) {
                Spacer(
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth()
                        .placeholderEffect()
                )
            }
        }
    }
}