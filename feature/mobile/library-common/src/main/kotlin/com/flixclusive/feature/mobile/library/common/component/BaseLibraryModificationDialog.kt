package com.flixclusive.feature.mobile.library.common.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.common.provider.extensions.asStatusColor
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.presentation.common.theme.Elevations
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.components.material3.dialog.CommonAlertDialog
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.library.common.R
import com.flixclusive.feature.mobile.library.common.model.TrackerProvider
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun BaseLibraryModificationDialog(
    label: String,
    name: () -> String,
    description: String?,
    confirmLabel: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isEditing: Boolean = false,
    selectedTracker: TrackerProvider? = null,
    availableTrackers: () -> Async<List<TrackerProvider>> = { Async.Success(emptyList()) },
    onTrackerChange: (TrackerProvider?) -> Unit = {},
) {
    val labelStyle = MaterialTheme.typography.labelLarge
        .copy(color = LocalContentColor.current.copy(0.6f))
        .asAdaptiveTextStyle(increaseBy = 2.sp)

    val textFieldStyle = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(increaseBy = 2.sp)

    val buttonMinHeight = getAdaptiveDp(50.dp)
    val buttonShape = MaterialTheme.shapes.small

    var showTrackerOptionDialog by remember { mutableStateOf(false) }

    val hideTrackerSheetToggleButton by remember(availableTrackers) {
        derivedStateOf {
            when (val trackers = availableTrackers()) {
                is Async.Loading -> true
                is Async.Failure -> true
                is Async.Success -> trackers.data.isEmpty()
            }
        }
    }

    val isCreateButtonEnabled by remember {
        derivedStateOf { name().isNotBlank() }
    }

    CommonAlertDialog(
        onDismiss = onCancel,
        action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp),
            ) {
                TextButton(
                    onClick = onCancel,
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight),
                ) {
                    Text(text = stringResource(LocaleR.string.label_cancel))
                }

                Button(
                    enabled = isCreateButtonEnabled,
                    onClick = onConfirm,
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight),
                ) {
                    Text(text = confirmLabel)
                }
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.asAdaptiveTextStyle(increaseBy = 2.sp),
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                if (!isEditing && !hideTrackerSheetToggleButton) {
                    Text(
                        text = stringResource(R.string.tracker_to_sync_with),
                        style = labelStyle,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    TrackerSheetToggle(
                        selectedTracker = selectedTracker,
                        onClick = { showTrackerOptionDialog = true },
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = stringResource(LocaleR.string.name),
                    style = labelStyle,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                TextField(
                    value = name(),
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = textFieldStyle,
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(LocaleR.string.description),
                    style = labelStyle,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                TextField(
                    value = description ?: "",
                    onValueChange = onDescriptionChange,
                    textStyle = textFieldStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = getAdaptiveDp(100.dp)),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = MaterialTheme.shapes.small,
                )
            }
        },
    )

    if (showTrackerOptionDialog) {
        TrackerSelectionSheet(
            trackers = availableTrackers(),
            onDismiss = { showTrackerOptionDialog = false },
            onTrackerSelect = {
                onTrackerChange(it)
                showTrackerOptionDialog = false
            }
        )
    }
}

@Composable
private fun TrackerSelectionSheet(
    trackers: Async<List<TrackerProvider>>,
    onTrackerSelect: (TrackerProvider?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    CommonBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.tracker_selector_desc),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(0.7f),
            modifier = Modifier.padding(vertical = 4.dp)
        )

        AsyncAnimatedContent(
            targetState = trackers,
            modifier = Modifier.padding(vertical = 12.dp),
            loadingContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        TrackerProviderCardPlaceholder()
                    }
                }
            },
            errorContent = {
                EmptyDataMessage(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    title = stringResource(com.flixclusive.core.presentation.mobile.R.string.an_error_occurred),
                    description = it.message.asString(),
                    icon = {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.round_error_outline_24),
                            contentDescription = stringResource(LocaleR.string.error_icon_content_desc),
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.error.copy(0.6f),
                        )
                    },
                )
            }
        ) { data ->
            TrackerProvidersList(
                trackers = data(),
                onSelect = onTrackerSelect,
            )
        }
    }
}

@Composable
private fun TrackerSheetToggle(
    selectedTracker: TrackerProvider?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(bottom = 4.dp),
    ) {
        AnimatedContent(
            targetState = selectedTracker,
            modifier = Modifier.weight(1f)
        ) { tracker ->
            if (tracker == null) {
                Text(
                    text = stringResource(LocaleR.string.none),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
            } else {
                Row {
                    ImageWithSmallPlaceholder(
                        model = remember { context.buildImageRequest(tracker.iconUrl) },
                        placeholder = painterResource(UiCommonR.drawable.provider_logo),
                        contentDescription = tracker.name,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                            .align(Alignment.CenterVertically),
                    )

                    Text(
                        text = tracker.name,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }

        Icon(
            painter = painterResource(UiCommonR.drawable.arrow_right_thin),
            contentDescription = stringResource(R.string.select_tracker),
            tint = LocalContentColor.current.copy(0.6f),
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun TrackerSheetItem(
    tracker: TrackerProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    TextButton(
        onClick = onClick,
        enabled = tracker.status.isWorking,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.padding(bottom = 4.dp),
    ) {
        ImageWithSmallPlaceholder(
            model = remember { context.buildImageRequest(tracker.iconUrl) },
            placeholder = painterResource(UiCommonR.drawable.provider_logo),
            contentDescription = tracker.name,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(40.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = tracker.name,
                style = MaterialTheme.typography.labelMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "v${tracker.versionName} (${tracker.versionCode})",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(0.6F)
                )

                if (tracker.status != ProviderStatus.Working) {
                    Text(
                        text = tracker.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = tracker.status.asStatusColor(),
                        modifier = Modifier
                            .graphicsLayer { alpha = 0.6F }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackerProvidersList(
    trackers: List<TrackerProvider>,
    onSelect: (TrackerProvider?) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val sortedTrackers by remember(trackers) {
        derivedStateOf {
            trackers.sortedBy { it.name }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fadingEdge(
            scrollableState = listState,
            orientation = Orientation.Vertical,
            startEdge = 30.dp,
            endEdge = 0.dp
        )
    ) {
        if (sortedTrackers.isNotEmpty()) {
            item {
                TextButton(
                    onClick = { onSelect(null) },
                    contentPadding = PaddingValues(0.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(LocaleR.string.none),
                            style = MaterialTheme.typography.labelMedium
                        )

                        Text(
                            text = "Use without tracker synchronization",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalContentColor.current.copy(0.6F)
                        )
                    }
                }
            }
        }

        items(
            sortedTrackers,
            key = { it.id }
        ) { tracker ->
            TrackerSheetItem(
                tracker = tracker,
                onClick = { onSelect(tracker) },
            )
        }
    }
}

@Composable
private fun TrackerProviderCardPlaceholder(modifier: Modifier = Modifier) {
    Placeholder(
        elevation = Elevations.LEVEL_3,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(40.dp + 12.dp)
    )
}

@Preview
@Composable
private fun TrackerSheetBottomPreview() {
    FlixclusiveTheme {
        Surface {
            TrackerSelectionSheet(
                trackers = Async.Success(
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
                ),
                onDismiss = {},
                onTrackerSelect = {},
            )
        }
    }
}

@Preview
@Composable
private fun TrackerSheetItemPreview() {
    FlixclusiveTheme {
        Surface {
            TrackerSheetItem(
                tracker = TrackerProvider(
                    isTrackerEnabled = false,
                    isAuthenticated = true,
                    metadata = DummyDataForPreview.getProviderMetadata()
                ),
                onClick = {},
            )
        }
    }
}
