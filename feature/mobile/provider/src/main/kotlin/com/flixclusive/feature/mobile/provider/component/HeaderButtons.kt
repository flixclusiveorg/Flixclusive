package com.flixclusive.feature.mobile.provider.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun HeaderButtons(
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ) {
        CustomButton(
            iconId = UiCommonR.drawable.upload,
            label = stringResource(id = UtilR.string.import_label),
            modifier = Modifier
                .weight(1F)
        )

        CustomButton(
            iconId = UiCommonR.drawable.download,
            label = stringResource(id = UtilR.string.export_label),
            modifier = Modifier
                .weight(1F)
        )
    }
}


@Composable
private fun CustomButton(
    modifier: Modifier = Modifier,
    @DrawableRes iconId: Int,
    label: String
) {
    Button(
        onClick = { /*TODO*/ },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.onMediumEmphasis(0.6F),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(vertical = 15.dp),
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = label,
            modifier = Modifier
                .size(20.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(start = 5.dp)
        )
    }
}

@Preview
@Composable
private fun HeaderButtonsPreview() {
    FlixclusiveTheme {
        Surface {
            Row {
                HeaderButtons()
            }
        }
    }
}

