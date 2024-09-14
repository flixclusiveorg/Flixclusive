package com.flixclusive.feature.mobile.provider.info.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.provider.info.SUB_LABEL_SIZE
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun DescriptionBlock(
    modifier: Modifier = Modifier,
    description: String?
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Title(text = stringResource(id = LocaleR.string.description))

        Text(
            text = description ?: stringResource(id = LocaleR.string.default_description_msg),
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal,
                color = LocalContentColor.current.onMediumEmphasis(0.8F),
                fontSize = SUB_LABEL_SIZE,
                lineHeight = 16.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )
    }
}