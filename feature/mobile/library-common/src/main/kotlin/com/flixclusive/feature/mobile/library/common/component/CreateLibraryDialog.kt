package com.flixclusive.feature.mobile.library.common.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.library.common.model.TrackerProvider
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun CreateLibraryDialog(
    trackers: () -> Async<List<TrackerProvider>>,
    onCreate: (String, String?, TrackerProvider?) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf<String?>(null) }
    var selectedTracker by remember { mutableStateOf<TrackerProvider?>(null) }

    BaseLibraryModificationDialog(
        label = stringResource(LocaleR.string.new_list),
        name = { name },
        description = description,
        selectedTracker = selectedTracker,
        onNameChange = { name = it },
        onDescriptionChange = { description = it },
        onTrackerChange = { selectedTracker = it },
        availableTrackers = trackers,
        onConfirm = { onCreate(name, description, selectedTracker) },
        confirmLabel = stringResource(LocaleR.string.create),
        onCancel = onCancel
    )
}

@Preview
@Composable
private fun CreateLibraryDialogPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            CreateLibraryDialog(
                trackers = {
                    Async.Success(
                        List(10) {
                            TrackerProvider(
                                isTrackerEnabled = it % 3 != 0,
                                isAuthenticated = it % 2 != 0,
                                metadata = DummyDataForPreview.getProviderMetadata(
                                    id = it.toString(),
                                    name = "Tracker $it",
                                    versionName = "1.0.$it",
                                    status = if (it % 2 == 0) ProviderStatus.Working else ProviderStatus.Down
                                )
                            )
                        }
                    )
                },
                onCreate = { _, _, _ -> },
                onCancel = { }
            )
        }
    }
}
