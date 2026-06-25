package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.provider.extensions.asStatusColor
import com.flixclusive.core.presentation.common.extensions.ifElse
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.provider.details.util.getFlagFromLanguageCode
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType
import java.util.Locale
import com.flixclusive.core.strings.R as LocaleR

@Stable
private sealed class InfoChipType {
    abstract val label: String

    @Stable
    data class Default(
        override val label: String
    ) : InfoChipType()

    @Stable
    data class Elevated(
        override val label: String,
        val contentColor: Color,
        val containerColor: Color
    ) : InfoChipType()
}

@Composable
internal fun ProviderDetailsExtraChips(
    provider: ProviderMetadata,
    modifier: Modifier = Modifier
) {
    val chips = buildInfoChips(provider = provider)

    FlowRow(modifier) {
        chips.forEach { chip ->
            val containerColor = when (chip) {
                is InfoChipType.Default -> MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
                is InfoChipType.Elevated -> chip.containerColor
            }

            val contentColor = when (chip) {
                is InfoChipType.Default -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                is InfoChipType.Elevated -> chip.contentColor
            }

            InfoChip(
                label = chip.label,
                containerColor = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .padding(end = 5.dp, bottom = 5.dp)
                    .ifElse(
                        condition = chip is InfoChipType.Default,
                        ifTrueModifier = Modifier.border(
                            width = 0.3.dp,
                            color = contentColor.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.extraSmall
                        )
                    )
            )
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = contentColor.copy(alpha = 0.15f)
) {
    Text(
        text = label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = contentColor,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .shadow(elevation = 1.dp, shape = MaterialTheme.shapes.extraSmall)
            .background(
                color = containerColor,
                shape = MaterialTheme.shapes.extraSmall,
            ).padding(
                horizontal = 5.dp,
                vertical = 1.dp
            )
    )
}

@Composable
private fun buildInfoChips(provider: ProviderMetadata): List<InfoChipType> {
    val resources = LocalResources.current

    val adultContainerColor = Color(0xFF5B1212)
    val adultContentColor = Color(0xFFFF4141)
    val providerTypeColor = provider.status.asStatusColor()

    return remember(provider) {
        buildList {
            add(InfoChipType.Default("v${provider.versionName}"))

            if (provider.status != ProviderStatus.Working) {
                add(
                    InfoChipType.Elevated(
                        label = provider.status.name,
                        contentColor = providerTypeColor,
                        containerColor = providerTypeColor.copy(alpha = 0.15f)
                    )
                )
            }

            add(InfoChipType.Default(provider.providerType.toString()))

            val flagEmoji = getFlagFromLanguageCode(provider.language.code)
            val language = Locale
                .Builder()
                .setLanguageTag(provider.language.code)
                .build()

            val displayLanguage = language
                .getDisplayLanguage(Locale.getDefault())
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            add(InfoChipType.Default("$flagEmoji $displayLanguage"))

            if (provider.adult) {
                add(
                    InfoChipType.Elevated(
                        label = resources.getString(LocaleR.string.label_provider_adult),
                        contentColor = adultContentColor,
                        containerColor = adultContainerColor.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProviderDetailsExtraChipsPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxWidth()
        ) {
            ProviderDetailsExtraChips(
                provider = DummyDataForPreview.getProviderMetadata(
                    adult = true,
                    status = ProviderStatus.Beta,
                    providerType = ProviderType("Addons, Adult, Movies, TV Shows"),
                    language = Language("random")
                )
            )
        }
    }
}
