package com.flixclusive.core.presentation.mobile.components.provider

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.drawables.R as UiCommonR

@SuppressLint("ModifierParameter")
@Suppress("compose:modifier-naming")
@Composable
fun ProviderTopCardContent(
    providerMetadata: ProviderMetadata,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ImageWithSmallPlaceholder(
            modifier = Modifier.size(60.dp),
            placeholderSize = 30.dp,
            urlImage = providerMetadata.iconUrl,
            placeholder = painterResource(UiCommonR.drawable.provider_logo),
            contentDescription = providerMetadata.name,
            shape = MaterialTheme.shapes.small,
        )

        ProviderDetails(
            providerMetadata = providerMetadata,
            modifier =
                Modifier
                    .weight(1F)
                    .padding(start = 5.dp),
        )
    }
}

@Composable
private fun ProviderDetails(
    providerMetadata: ProviderMetadata,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current

    Column(
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = providerMetadata.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp),
            )

            Text(
                text = "v${providerMetadata.versionName}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = LocalContentColor.current.copy(0.4F),
                        fontSize = 13.sp,
                    ),
            )
        }

        Text(
            text = providerMetadata.providerType.toString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = LocalContentColor.current.copy(0.6f),
                    fontSize = 13.sp,
                ),
        )
    }
}

@Preview
@Composable
private fun ProviderTopCardContentPreview() {
    FlixclusiveTheme {
        Surface {
            ProviderTopCardContent(
                providerMetadata = DummyDataForPreview.getProviderMetadata(),
            )
        }
    }
}
