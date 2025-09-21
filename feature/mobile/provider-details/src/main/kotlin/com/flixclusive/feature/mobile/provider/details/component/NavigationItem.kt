package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.provider.details.util.ProviderDetailsUiCommon.HORIZONTAL_PADDING
import com.flixclusive.feature.mobile.provider.details.util.ProviderDetailsUiCommon.LABEL_SIZE_IN_DP
import com.flixclusive.core.presentation.mobile.R as UiMobileR
import com.flixclusive.core.strings.R as LocaleR


@Composable
internal fun NavigationItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
                AdaptiveIcon(
                    painter = painterResource(id = UiMobileR.drawable.right_arrow),
                    contentDescription = stringResource(id = LocaleR.string.see_all),
                    tint = LocalContentColor.current.copy(0.6f),
                    dp = LABEL_SIZE_IN_DP,
                    increaseBy = 2.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
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
