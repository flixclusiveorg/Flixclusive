package com.flixclusive.feature.mobile.library.details.component

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.strings.R
import com.flixclusive.feature.mobile.library.details.LibraryType
import java.text.SimpleDateFormat
import java.util.Locale
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ScreenHeader(
    library: LibraryList,
    libraryType: LibraryType,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val actualLibraryInfo = remember(library) {
        context.getActualLibraryInfo(
            library = library,
            libraryType = libraryType
        )
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = actualLibraryInfo.name,
            style = MaterialTheme.typography.headlineMedium
        )

        actualLibraryInfo.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(0.6f),
            )
        }

        if (libraryType == LibraryType.Custom) {
            MetadataItem(
                label = stringResource(R.string.created_at),
                value = dateFormatter.format(library.createdAt),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (library.updatedAt.time > 0L) {
            MetadataItem(
                label = stringResource(R.string.modified_at),
                value = dateFormatter.format(library.updatedAt),
            )
        }
    }
}

@Composable
private fun MetadataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.copy(0.4f),
        )
    }
}

private fun Context.getActualLibraryInfo(
    library: LibraryList,
    libraryType: LibraryType
): LibraryList {
    return when (libraryType) {
        LibraryType.Watchlist -> library.copy(
            name = getString(LocaleR.string.watchlist),
            description = getString(LocaleR.string.watchlist_description)
        )
        LibraryType.WatchHistory -> library.copy(
            name = getString(LocaleR.string.recently_watched),
            description = getString(LocaleR.string.recently_watched_description)
        )
        else -> library
    }
}

