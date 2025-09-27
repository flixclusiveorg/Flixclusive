package com.flixclusive.feature.mobile.repository.manage.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.extensions.ifElse
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.model.provider.Repository
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

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
        if (repository.url.contains("github")) {
            "https://github.com/${repository.owner}.png"
        } else {
            null
        }
    }

    val heightModifier = Modifier.heightIn(getAdaptiveDp(50.dp))
    val iconSize = 20.dp

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier)
            .background(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1),
            ).ifElse(
                isSelected,
                Modifier.border(
                    BorderStroke(Dp.Hairline, tertiary),
                    shape = MaterialTheme.shapes.small,
                ),
            ).combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ).padding(13.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
            modifier = Modifier
                .align(Alignment.Top)
                .padding(end = 12.dp)
                .weight(1F),
        ) {
            Text(
                text = repository.name,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ImageWithSmallPlaceholder(
                    modifier = Modifier.size(16.dp),
                    placeholderSize = 9.dp,
                    urlImage = image,
                    placeholderId = UiCommonR.drawable.repository,
                    contentDescId = LocaleR.string.owner_avatar_content_desc,
                )

                Text(
                    text = repository.owner,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(size = 13.sp),
                )
            }
        }

        CustomIconButton(
            description = stringResource(id = LocaleR.string.open_in_web),
            onClick = { uriHandler.openUri(repository.url) },
        ) {
            AdaptiveIcon(
                painter = painterResource(id = UiCommonR.drawable.web_browser),
                contentDescription = stringResource(id = LocaleR.string.open_in_web),
                dp = iconSize,
                tint = LocalContentColor.current.copy(0.6f),
            )
        }

        CustomIconButton(
            description = stringResource(id = LocaleR.string.copy_link),
            onClick = { onCopy(repository.url) },
        ) {
            AdaptiveIcon(
                painter = painterResource(id = UiCommonR.drawable.round_content_copy_24),
                contentDescription = stringResource(id = LocaleR.string.copy_link),
                dp = iconSize,
                tint = LocalContentColor.current.copy(0.6f),
            )
        }

        CustomIconButton(
            description = stringResource(id = LocaleR.string.delete_repository),
            onClick = onDeleteRepository,
        ) {
            AdaptiveIcon(
                painter = painterResource(id = UiCommonR.drawable.outlined_trash),
                contentDescription = stringResource(id = LocaleR.string.delete_repository),
                dp = iconSize,
                tint = LocalContentColor.current.copy(0.6f),
            )
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
                    "",
                ),
                isSelected = true,
                onClick = {},
                onLongClick = {},
                onDeleteRepository = {},
                onCopy = {},
            )
        }
    }
}
