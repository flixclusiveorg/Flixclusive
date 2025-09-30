package com.flixclusive.feature.mobile.provider.manage.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun HeaderButtons(
    onImport: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        HeaderButton(
            onClick = onImport,
            iconId = UiCommonR.drawable.upload,
            label = stringResource(id = LocaleR.string.import_label),
            modifier = Modifier.weight(1F),
        )

        HeaderButton(
            onClick = onExport,
            iconId = UiCommonR.drawable.download,
            label = stringResource(id = LocaleR.string.export_label),
            modifier = Modifier.weight(1F),
        )
    }
}

@Composable
private fun HeaderButton(
    onClick: () -> Unit,
    @DrawableRes iconId: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(level = 3),
            contentColor = MaterialTheme.colorScheme.onSurface.copy(0.8F),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 20.dp),
        contentPadding = PaddingValues(vertical = 15.dp),
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        AdaptiveIcon(
            painter = painterResource(id = iconId),
            contentDescription = label,
            dp = 20.dp,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

@Preview
@Composable
private fun HeaderButtonsPreview() {
    FlixclusiveTheme {
        Surface {
            Row {
                HeaderButtons(
                    onImport = {},
                    onExport = {},
                )
            }
        }
    }
}
