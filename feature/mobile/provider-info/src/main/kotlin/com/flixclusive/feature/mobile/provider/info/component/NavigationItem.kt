package com.flixclusive.feature.mobile.provider.info.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.provider.info.HORIZONTAL_PADDING
import com.flixclusive.feature.mobile.provider.info.LABEL_SIZE_IN_DP
import com.flixclusive.core.ui.mobile.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun NavigationItem(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = HORIZONTAL_PADDING)
                .fillMaxWidth()
        ) {
            Title(text = label)

            Box {
                Icon(
                    painter = painterResource(id = UiCommonR.drawable.right_arrow),
                    contentDescription = stringResource(id = LocaleR.string.see_all),
                    tint = LocalContentColor.current.onMediumEmphasis(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(LABEL_SIZE_IN_DP)
                        .clickable(onClick = onClick)
                )
            }
        }
    }
}

@Preview
@Composable
private fun NavigationItemPreview() {
    FlixclusiveTheme {
        Surface {
            NavigationItem(
                label = stringResource(id = LocaleR.string.whats_new),
                onClick = {}
            )
        }
    }
}