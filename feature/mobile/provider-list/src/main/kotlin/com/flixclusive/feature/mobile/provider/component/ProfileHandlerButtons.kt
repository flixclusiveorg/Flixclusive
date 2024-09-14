package com.flixclusive.feature.mobile.provider.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun ProfileHandlerButtons(
    modifier: Modifier = Modifier,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ) {
        CustomButton(
            onClick = onImport,
            iconId = UiCommonR.drawable.upload,
            label = stringResource(id = LocaleR.string.import_label),
            modifier = Modifier
                .weight(1F)
        )

        CustomButton(
            onClick = onExport,
            iconId = UiCommonR.drawable.download,
            label = stringResource(id = LocaleR.string.export_label),
            modifier = Modifier
                .weight(1F)
        )
    }
}

@Preview
@Composable
private fun HeaderButtonsPreview() {
    FlixclusiveTheme {
        Surface {
            Row {
                ProfileHandlerButtons(
                    onImport = {},
                    onExport = {}
                )
            }
        }
    }
}

