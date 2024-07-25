package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun TestResultsDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        HorizontalDivider(
            color = LocalContentColor.current.onMediumEmphasis(0.4F),
            thickness = 0.5.dp,
            modifier = Modifier.weight(1F)
        )
        
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = UtilR.string.test_results),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
            )
        }

        HorizontalDivider(
            color = LocalContentColor.current.onMediumEmphasis(0.4F),
            thickness = 0.5.dp,
            modifier = Modifier.weight(1F)
        )
    }
}

@Preview
@Composable
private fun TestScreenDividerPreview() {
    FlixclusiveTheme {
        Surface {
            TestResultsDivider()
        }
    }
}