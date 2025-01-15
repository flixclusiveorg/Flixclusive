package com.flixclusive.feature.mobile.repository.manage.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flixclusive.model.provider.Repository
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RepositoryCard(
    repository: Repository,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteRepository: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val tertiary = MaterialTheme.colorScheme.tertiary
    val image = remember {
        if (repository.url.contains("github")) "https://github.com/${repository.owner}.png"
        else null
    }

    val heightModifier = Modifier.heightIn(getAdaptiveDp(50.dp))

    Card(
        shape = MaterialTheme.shapes.small,
        border = if (isSelected) BorderStroke(Dp.Hairline, tertiary) else null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        elevation = CardDefaults.cardElevation(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = heightModifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(end = 12.dp)
                    .weight(1F)
            ) {
                Text(
                    text = repository.name,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = getAdaptiveTextStyle(
                        mode = TextStyleMode.Emphasized,
                        style = TypographyStyle.Title
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ImageWithSmallPlaceholder(
                        modifier = Modifier.size(16.dp),
                        placeholderModifier = Modifier.size(9.dp),
                        urlImage = image,
                        placeholderId = UiCommonR.drawable.repository,
                        contentDescId = LocaleR.string.owner_avatar_content_desc
                    )

                    Text(
                        text = repository.owner,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        style = getAdaptiveTextStyle(
                            mode = TextStyleMode.Light,
                            size = 13.sp
                        )
                    )
                }
            }

            CustomIconButton(
                description = stringResource(id = LocaleR.string.open_in_web),
                onClick = { uriHandler.openUri(repository.url) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.web_browser),
                    contentDescription = stringResource(id = LocaleR.string.open_in_web),
                    tint = LocalContentColor.current.onMediumEmphasis()
                )
            }

            CustomIconButton(
                description = stringResource(id = LocaleR.string.copy_link),
                onClick = { onCopy(repository.url) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.round_content_copy_24),
                    contentDescription = stringResource(id = LocaleR.string.copy_link),
                    tint = LocalContentColor.current.onMediumEmphasis()
                )
            }

            CustomIconButton(
                description = stringResource(id = LocaleR.string.delete_repository),
                onClick = onDeleteRepository
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.outlined_trash),
                    contentDescription = stringResource(id = LocaleR.string.delete_repository),
                    tint = LocalContentColor.current.onMediumEmphasis()
                )
            }
        }
    }
}


@Preview
@Composable
private fun RepositoryCardPreview() {
    FlixclusiveTheme {
        Surface {
            RepositoryCard(
                repository = Repository(
                    "rhenwinch",
                    "providers",
                    "https://github.com/flixclusiveorg/providers",
                    ""
                ),
                isSelected = true,
                onClick = {},
                onLongClick = {},
                onDeleteRepository = {},
                onCopy = {}
            )
        }
    }
}
