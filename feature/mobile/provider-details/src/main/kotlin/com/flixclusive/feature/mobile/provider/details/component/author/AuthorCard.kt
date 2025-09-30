package com.flixclusive.feature.mobile.provider.details.component.author

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.ifElse
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.provider.details.util.ProviderDetailsUiCommon.SUB_LABEL_SIZE
import com.flixclusive.model.provider.Author
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun AuthorCard(
    author: Author,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .ifElse(
                condition = author.socialLink != null,
                ifTrueModifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        author.socialLink?.let(uriHandler::openUri)
                    }
                )
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.CenterStart)
        ) {
            val avatarSize = 45.dp
            ImageWithSmallPlaceholder(
                modifier = Modifier
                    .size(avatarSize)
                    .ifElse(
                        condition = author.socialLink != null,
                        ifTrueModifier = Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(
                                bounded = false,
                                radius = avatarSize / 2
                            ),
                            onClick = {
                                author.socialLink?.let(uriHandler::openUri)
                            }
                        )
                    ),
                urlImage = author.image,
                placeholderId = UiCommonR.drawable.profile_placeholder,
                contentDescId = LocaleR.string.author_icon_content_desc,
            )

            Text(
                text = author.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(SUB_LABEL_SIZE)
            )
        }
    }
}
