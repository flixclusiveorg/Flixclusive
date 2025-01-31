package com.flixclusive.feature.mobile.library.details.component

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.locale.R
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.model.database.LibraryList
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun ScreenHeader(
    library: LibraryList,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = library.name,
            style = MaterialTheme.typography.headlineMedium
        )

        library.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.onMediumEmphasis(),
            )
        }

        MetadataItem(
            label = stringResource(R.string.created_at),
            value = dateFormatter.format(library.createdAt),
            modifier = Modifier.padding(top = 8.dp)
        )

        MetadataItem(
            label = stringResource(R.string.modified_at),
            value = dateFormatter.format(library.updatedAt),
        )
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
            color = LocalContentColor.current.onMediumEmphasis(0.4f),
        )
    }
}

