package com.flixclusive.feature.mobile.profiles

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun EmptyScreen(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit
) {
    EmptyDataMessage(
        modifier = modifier.fillMaxSize(),
        title = stringResource(LocaleR.string.whos_watching),
        description = stringResource(LocaleR.string.empty_user_profiles_list_message),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(50.dp)
                .border(
                    width = 1.dp,
                    shape = MaterialTheme.shapes.small,
                    color = LocalContentColor.current.copy(0.6f)
                )
                .clickable(onClick = onAdd)
        ) {
            Icon(
                painter = painterResource(id = UiCommonR.drawable.round_add_24),
                contentDescription = stringResource(LocaleR.string.add_providers),
                tint = LocalContentColor.current.copy(0.4F),
                modifier = Modifier.size(35.dp)
            )
        }
    }
}
@Preview
@Composable
private fun EmptyScreenPreview() {
    FlixclusiveTheme {
        Surface {
            EmptyScreen(onAdd = {})
        }
    }
}
