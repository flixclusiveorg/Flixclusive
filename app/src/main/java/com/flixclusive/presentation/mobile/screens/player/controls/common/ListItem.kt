package com.flixclusive.presentation.mobile.screens.player.controls.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.domain.common.Resource
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.utils.ComposeUtils.applyDropShadow
import com.flixclusive.presentation.utils.ModifierUtils.placeholderEffect

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    name: String,
    index: Int,
    selectedIndex: Int,
    itemState: Resource<Any?> = Resource.Success(null),
    onClick: () -> Unit,
) {
    val baseStyle = MaterialTheme.typography.labelLarge
    val unselectedColor = colorOnMediumEmphasisMobile()

    val style = remember(selectedIndex) {
        if (selectedIndex == index)
            baseStyle.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ).applyDropShadow()
        else baseStyle.copy(
            fontWeight = FontWeight.Bold,
            color = unselectedColor
        ).applyDropShadow()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(enabled = index != selectedIndex) {
                onClick()
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (selectedIndex == index) {
                when (itemState) {
                    is Resource.Failure -> Unit
                    Resource.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(40.dp)
                                .padding(start = 25.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 1.5.dp,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }
                    }

                    is Resource.Success -> {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Check indicator for selected sheet item",
                            modifier = Modifier.padding(start = 25.dp)
                        )
                    }
                }
            }

            Text(
                text = name,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 60.dp)
            )
        }
    }
}

@Composable
fun SheetItemPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 60.dp)
        ) {
            Spacer(
                Modifier
                    .placeholderEffect()
                    .height(14.dp)
                    .width(60.dp)
            )
        }
    }
}

@Preview
@Composable
fun SheetItemPreview() {
    FlixclusiveMobileTheme {
        Surface {
            ListItem(
                name = "Superstream",
                index = 0,
                selectedIndex = 0,
                itemState = Resource.Loading,
                onClick = {}
            )
        }
    }
}