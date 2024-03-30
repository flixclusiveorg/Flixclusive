package com.flixclusive.feature.mobile.provider.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flixclusive.gradle.entities.Author
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun AuthorCard(
    modifier: Modifier = Modifier,
    author: Author,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .ifElse(
                condition = author.githubLink != null,
                ifTrueModifier = Modifier.clickable {
                    uriHandler.openUri(author.githubLink!!)
                }
            )
    ) {
        ImageWithSmallPlaceholder(
            modifier = Modifier
                .size(45.dp),
            placeholderModifier = Modifier.fillMaxSize(),
            urlImage = "${author.githubLink}.png",
            placeholderId = UiCommonR.drawable.profile_placeholder,
            contentDescId = UtilR.string.author_icon_content_desc,
        )

        Text(
            text = author.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        )
    }
}