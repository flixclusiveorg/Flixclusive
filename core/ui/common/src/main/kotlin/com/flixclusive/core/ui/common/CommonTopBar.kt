package com.flixclusive.core.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.flixclusive.core.locale.R as LocaleR

val COMMON_TOP_BAR_HEIGHT = 65.dp

@Composable
fun CommonTopBar(
    modifier: Modifier = Modifier,
    headerTitle: String,
    onNavigationIconClick: () -> Unit,
    endContent: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .height(COMMON_TOP_BAR_HEIGHT),
        contentAlignment = Alignment.TopCenter
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(65.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    painter = painterResource(R.drawable.left_arrow),
                    contentDescription = stringResource(LocaleR.string.navigate_up)
                )
            }

            Text(
                text = headerTitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .weight(1F)
                    .padding(start = 15.dp)
            )

            endContent?.invoke()
        }
    }
}