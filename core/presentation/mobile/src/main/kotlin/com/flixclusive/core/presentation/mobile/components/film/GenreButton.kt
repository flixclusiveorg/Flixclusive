package com.flixclusive.core.presentation.mobile.components.film

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.model.film.Genre

@Composable
fun GenreButton(
    genre: Genre,
    onClick: (Genre) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = LocalContentColor.current.copy(0.6f),
                shape = CircleShape,
            ).background(
                color = Color.Transparent,
                shape = CircleShape,
            ).clickable { onClick(genre) },
    ) {
        Text(
            text = genre.name,
            style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
            color = LocalContentColor.current.copy(0.6f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    horizontal = getAdaptiveDp(15.dp, 2.dp),
                    vertical = getAdaptiveDp(4.dp, 2.dp),
                ),
        )
    }
}

@Preview
@Composable
private fun GenreButtonBasePreview() {
    FlixclusiveTheme {
        Surface {
            GenreButton(
                genre = Genre(1, "Action"),
                onClick = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun GenreButtonCompactLandscapePreview() {
    GenreButtonBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun GenreButtonMediumPortraitPreview() {
    GenreButtonBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun GenreButtonMediumLandscapePreview() {
    GenreButtonBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun GenreButtonExtendedPortraitPreview() {
    GenreButtonBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun GenreButtonExtendedLandscapePreview() {
    GenreButtonBasePreview()
}
