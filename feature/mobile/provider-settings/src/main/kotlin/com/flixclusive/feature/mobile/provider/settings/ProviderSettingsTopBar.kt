package com.flixclusive.feature.mobile.provider.settings


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

internal val TOP_BAR_HEIGHT = 65.dp

@Composable
internal fun ProviderSettingsTopBar(
    modifier: Modifier = Modifier,
    label: String,
    onNavigationIconClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .height(TOP_BAR_HEIGHT),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.left_arrow),
                    contentDescription = stringResource(UtilR.string.navigate_up)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .weight(1F)
                    .padding(horizontal = 15.dp)
            )
        }
    }
}