package com.flixclusive.feature.mobile.repository.search.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flixclusive.model.provider.Repository
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.ui.mobile.R as UiMobileR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RepositoryCard(
    modifier: Modifier = Modifier,
    repository: Repository,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val border = remember(isSelected) {
        when {
            isSelected -> BorderStroke(2.dp, tertiary)
            else -> null
        }
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        elevation = CardDefaults.cardElevation(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageWithSmallPlaceholder(
                modifier = Modifier.size(70.dp),
                placeholderModifier = Modifier.size(40.dp),
                urlImage = if (repository.url.contains("github")) "https://github.com/${repository.owner}.png" else null,
                placeholderId = UiCommonR.drawable.repository,
                contentDescId = LocaleR.string.owner_avatar_content_desc
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1F)
            ) {
                Text(
                    text = repository.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Text(
                    text = repository.owner,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            Icon(
                painter = painterResource(id = UiMobileR.drawable.right_arrow),
                contentDescription = stringResource(id = LocaleR.string.navigate_to_repository_content_desc)
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
                    ""
                ),
                isSelected = true,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}