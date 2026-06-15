package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.mobile.R as UiMobileR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun NavigationItem(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(40.dp)
                .padding(vertical = 2.dp, horizontal = 5.dp)
                .fillMaxWidth()
        ) {
            Icon(
                painter = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                color = LocalContentColor.current,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )

            Icon(
                painter = painterResource(id = UiMobileR.drawable.right_arrow),
                contentDescription = stringResource(id = LocaleR.string.navigate),
                tint = LocalContentColor.current.copy(0.6f),
                modifier = Modifier.size(12.dp)
            )
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
                icon = painterResource(id = UiCommonR.drawable.github_outline),
                onClick = {}
            )
        }
    }
}
