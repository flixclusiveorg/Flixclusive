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
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.EmptyDataMessage
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

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
                .clickable(onClick = onAdd)
                .size(50.dp)
                .border(
                    width = 1.dp,
                    shape = MaterialTheme.shapes.small,
                    color = LocalContentColor.current.onMediumEmphasis()
                )
        ) {
            Icon(
                painter = painterResource(id = UiCommonR.drawable.round_add_24),
                contentDescription = stringResource(LocaleR.string.add_providers),
                tint = LocalContentColor.current.onMediumEmphasis(0.4F),
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@Preview
@Composable
private fun EmptyScreen2Preview() {

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
