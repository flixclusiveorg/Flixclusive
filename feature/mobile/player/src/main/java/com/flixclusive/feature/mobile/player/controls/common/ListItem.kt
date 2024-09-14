package com.flixclusive.feature.mobile.player.controls.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.applyDropShadow
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.placeholderEffect
import com.flixclusive.core.ui.player.PlayerProviderState
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ListItem(
    modifier: Modifier = Modifier,
    name: String,
    index: Int,
    selectedIndex: Int,
    itemState: PlayerProviderState = PlayerProviderState.SELECTED,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val baseStyle = MaterialTheme.typography.labelLarge
    val unselectedColor = LocalContentColor.current.onMediumEmphasis()

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

    Box(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            if (selectedIndex == index) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    when (itemState) {
                        PlayerProviderState.LOADING -> {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 1.5.dp,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .size(18.dp)
                            )
                        }
                        PlayerProviderState.SELECTED -> {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = stringResource(LocaleR.string.check_indicator_content_desc),
                                modifier = Modifier
                                    .size(18.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(
                    modifier = Modifier.width(18.dp)
                )
            }

            Text(
                text = name,
                style = style,
                overflow = TextOverflow.Ellipsis
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

@Preview
@Composable
private fun SheetItemLoadingPreview() {
    FlixclusiveTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                repeat(3) {
                    ListItem(
                        name = "FLX",
                        index = 0,
                        selectedIndex = if (it < 2) 0 else 1,
                        itemState = PlayerProviderState.entries[it % PlayerProviderState.entries.size],
                        onClick = {},
                        onLongClick = {}
                    )
                }
            }

        }
    }
}