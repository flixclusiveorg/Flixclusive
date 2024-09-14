package com.flixclusive.feature.tv.player.controls.settings.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.placeholderEffect
import com.flixclusive.core.ui.player.PlayerProviderState
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ListItem(
    modifier: Modifier = Modifier,
    name: String,
    index: Int,
    selectedIndex: Int,
    itemState: PlayerProviderState,
    onClick: () -> Unit,
) {
    val baseStyle = MaterialTheme.typography.labelLarge
    val unselectedColor = LocalContentColor.current.onMediumEmphasis(0.4F)

    val interactionSource = remember { MutableInteractionSource() }

    val style = remember(selectedIndex) {
        if (selectedIndex == index)
            baseStyle.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
        else baseStyle.copy(
            fontWeight = FontWeight.Bold,
            color = unselectedColor,
            fontSize = 20.sp
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = unselectedColor,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.3F),
        interactionSource = interactionSource
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn()
        ) {
            if (selectedIndex == index) {
                when (itemState) {
                    PlayerProviderState.LOADING -> {
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
                    PlayerProviderState.SELECTED -> {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(LocaleR.string.check_indicator_content_desc),
                        )
                    }
                }
            } else {
                Spacer(Modifier.width(if(itemState == PlayerProviderState.LOADING) 35.dp else 20.dp))
            }

            Text(
                text = name,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current
            )
        }

    }
}

@Composable
private fun SheetItemPlaceholder() {
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun SheetItemPreview() {
    FlixclusiveTheme(isTv = true) {
        Surface {
            ListItem(
                name = "Superstream",
                index = 0,
                selectedIndex = 0,
                itemState = PlayerProviderState.LOADING,
                onClick = {}
            )
        }
    }
}