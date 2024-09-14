package com.flixclusive.feature.mobile.provider.info.component.subdetails

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.DummyDataForPreview
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.UiText
import com.flixclusive.feature.mobile.provider.info.HORIZONTAL_PADDING
import com.flixclusive.model.provider.ProviderData
import java.util.Locale
import com.flixclusive.core.locale.R as LocaleR

private fun String.capitalize(): String {
    return replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.US)
        else it.toString()
    }
}

@Composable
internal fun SubDetailsList(
    providerData: ProviderData
) {
    val subDetails = remember(providerData) {
        listOf(
            providerData.versionName to UiText.StringResource(LocaleR.string.version),
            providerData.status.toString() to UiText.StringResource(LocaleR.string.status),
            Locale(providerData.language.languageCode).displayLanguage.capitalize() to UiText.StringResource(LocaleR.string.language),
            providerData.providerType.type to UiText.StringResource(LocaleR.string.content),
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 20.dp)
    ) {
        itemsIndexed(subDetails) { i, details ->
            AnimatedContent(
                targetState = details,
                transitionSpec = {
                     ContentTransform(
                         targetContentEnter = fadeIn(),
                         initialContentExit = fadeOut()
                     )
                },
                label = ""
            ) { (title, value) ->
                SubDetailsItem(
                    title = title,
                    subtitle = value.asString()
                )
            }

            if (i < subDetails.lastIndex) {
                VerticalDivider(
                    thickness = 1.dp,
                    color = LocalContentColor.current.onMediumEmphasis(0.4F),
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 25.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SubDetailsListPreview() {
    val providerData = DummyDataForPreview.getDummyProviderData()

    FlixclusiveTheme {
        Surface {
            SubDetailsList(providerData = providerData)
        }
    }
}