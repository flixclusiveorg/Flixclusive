package com.flixclusive.feature.mobile.player.component.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.dropShadow
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ListItem(
    modifier: Modifier = Modifier,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val baseStyle = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle()
    val unselectedColor = LocalContentColor.current.copy(0.6f)

    val style = remember(isSelected) {
        if (isSelected)
            baseStyle.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ).dropShadow()
        else baseStyle.copy(
            fontWeight = FontWeight.Bold,
            color = unselectedColor
        ).dropShadow()
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .combinedClickable(
                onClick = {
                    if (!isSelected) {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    AdaptiveIcon(
                        painter = painterResource(UiCommonR.drawable.check),
                        contentDescription = stringResource(LocaleR.string.selected),
                        dp = 18.dp
                    )
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
                        isSelected = it == 1,
                        onClick = {},
                        onLongClick = {}
                    )
                }
            }
        }
    }
}
