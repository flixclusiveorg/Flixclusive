package com.flixclusive.feature.mobile.repository.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.gradle.entities.Repository
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.ui.mobile.R as UiMobileR
import com.flixclusive.core.util.R as UtilR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RepositoryCard(
    modifier: Modifier = Modifier,
    repository: Repository,
    onClick: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RepositoryIcon(
                modifier = Modifier
                    .size(70.dp),
                repository = repository)

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
                contentDescription = stringResource(id = UtilR.string.navigate_to_repository_content_desc)
            )
        }
    }
}

@Composable
private fun RepositoryIcon(
    modifier: Modifier = Modifier,
    repository: Repository
) {
    Surface(
        modifier = modifier,
        tonalElevation = 65.dp,
        shape = CircleShape
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = UiCommonR.drawable.repository),
                contentDescription = stringResource(id = UtilR.string.owner_avatar_content_desc),
                tint = LocalContentColor.current.onMediumEmphasis(0.8F),
                modifier = Modifier
                    .size(40.dp)
            )

            if (repository.url.contains("github")) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://github.com/${repository.owner}.png")
                        .crossfade(true)
                        .build(),
                    imageLoader = LocalContext.current.imageLoader,
                    contentDescription = stringResource(id = UtilR.string.owner_avatar_content_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
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
                    "https://github.com/rhenwinch/providers",
                    ""
                ),
                onClick = {}
            )
        }
    }
}