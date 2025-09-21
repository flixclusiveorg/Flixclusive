package com.flixclusive.feature.mobile.provider.details.component

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.provider.details.util.ProviderDetailsUiCommon.SUB_LABEL_SIZE
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun DescriptionBlock(
    description: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Title(text = stringResource(id = LocaleR.string.description))

        Text(
            text = description ?: stringResource(id = LocaleR.string.default_description_msg),
            overflow = TextOverflow.Ellipsis,
            color = LocalContentColor.current.copy(0.8F),
            style = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(SUB_LABEL_SIZE),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )
    }
}
