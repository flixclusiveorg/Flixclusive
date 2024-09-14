package com.flixclusive.feature.mobile.provider.info.component.author


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.provider.info.HORIZONTAL_PADDING
import com.flixclusive.feature.mobile.provider.info.component.Title
import com.flixclusive.model.provider.Author
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun AuthorsList(
    modifier: Modifier = Modifier,
    authors: List<Author>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Title(
            text = stringResource(id = LocaleR.string.authors),
            modifier = Modifier
                .padding(horizontal = HORIZONTAL_PADDING)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 8.dp)
        ) {
            items(authors) {
                AuthorCard(
                    author = it,
                    modifier = Modifier
                        .widthIn(min = 60.dp, max = 85.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun AuthorsListPreview() {
    FlixclusiveTheme {
        Surface {
            AuthorsList(
                authors = List(5) {
                    Author(
                        name = "Captain Jack Sparrow",
                        socialLink = "https://github.com/johndoe"
                    )
                }
            )
        }
    }
}