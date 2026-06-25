package com.flixclusive.feature.mobile.library.manage.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.common.provider.extensions.asStatusColor
import com.flixclusive.core.presentation.common.components.GradientCircularProgressIndicator
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.presentation.common.theme.Elevations
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.library.common.model.TrackerProvider
import com.flixclusive.feature.mobile.library.manage.R
import com.flixclusive.model.provider.ProviderStatus
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private enum class TrackerAuthState {
    Unauthenticated,
    Authenticated,
    Authenticating
}

@Composable
internal fun TrackerProvidersBottomSheet(
    trackers: () -> Async<List<TrackerProvider>>,
    onDismiss: () -> Unit,
    onToggle: (TrackerProvider) -> Unit,
    openProviderSettings: (TrackerProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    CommonBottomSheet(onDismiss) {
        Text(
            text = stringResource(R.string.tracker_providers_sheet_label),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(R.string.tracker_providers_sheet_desc),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(0.7f),
            modifier = Modifier.padding(vertical = 4.dp)
        )

        AsyncAnimatedContent(
            targetState = trackers(),
            modifier = modifier.padding(vertical = 12.dp),
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
                openProviderSettings = openProviderSettings,
                onSave = { list ->
                    scope.launch { list.forEach(onToggle) }
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun TrackerProvidersList(
    trackers: List<TrackerProvider>,
    openProviderSettings: (TrackerProvider) -> Unit,
    onSave: (List<TrackerProvider>) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentTrackers = remember {
        mutableStateMapOf<String, TrackerProvider>().also {
            trackers.forEach { provider ->
                it[provider.id] = provider
            }
        }
    }

    val sortedTrackers by remember {
        derivedStateOf { currentTrackers.values.sortedBy { it.name } }
    }

    val density = LocalDensity.current
    var buttonHeight by remember { mutableStateOf(40.dp) }
    val isButtonEnabled by remember {
        derivedStateOf {
            currentTrackers.values.forEachIndexed { index, wrapper ->
                if (wrapper.isTrackerEnabled != trackers[index].isTrackerEnabled) {
                    return@derivedStateOf true
                }
            }

            false
        }
    }

    Box(modifier) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = buttonHeight,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fadingEdge(
                scrollableState = listState,
                orientation = Orientation.Vertical,
                startEdge = buttonHeight * 2f,
                endEdge = 0.dp
            )
        ) {
            items(
                sortedTrackers,
                key = { it.id }
            ) { tracker ->
                TrackerCard(
                    tracker = tracker,
                    enabled = { tracker.isTrackerEnabled },
                    openProviderSettings = { openProviderSettings(tracker) },
                    onToggle = {
                        val updatedTracker = tracker.copy(isTrackerEnabled = !tracker.isTrackerEnabled)
                        currentTrackers[tracker.id] = updatedTracker
                    },
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    buttonHeight = with(density) { coordinates.size.height.toDp() }
                }
        ) {
            Button(
                enabled = isButtonEnabled,
                shape = MaterialTheme.shapes.small,
                onClick = { onSave(sortedTrackers) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text(text = stringResource(LocaleR.string.save))
            }
        }
    }
}

@Composable
private fun TrackerCard(
    tracker: TrackerProvider,
    enabled: () -> Boolean,
    onToggle: () -> Unit,
    openProviderSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var signInButtonState by remember {
        mutableStateOf(
            when {
                tracker.isAuthenticated -> TrackerAuthState.Authenticated
                else -> TrackerAuthState.Unauthenticated
            }
        )
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        ),
        onClick = onToggle
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
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

            AnimatedContent(
                targetState = signInButtonState,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { status ->
                when (status) {
                    TrackerAuthState.Authenticating -> {
                        GradientCircularProgressIndicator(
                            size = 8.dp,
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            )
                        )
                    }

                    TrackerAuthState.Unauthenticated -> {
                        Button(
                            onClick = {
                                signInButtonState = TrackerAuthState.Authenticating
                                openProviderSettings()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.sign_in),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }

                    TrackerAuthState.Authenticated -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = openProviderSettings
                            ) {
                                AdaptiveIcon(
                                    painter = painterResource(id = UiCommonR.drawable.provider_settings),
                                    contentDescription = stringResource(id = LocaleR.string.provider_settings),
                                    tint = LocalContentColor.current.copy(0.4F)
                                )
                            }

                            Switch(
                                checked = enabled(),
                                enabled = tracker.status != ProviderStatus.Down,
                                colors = SwitchDefaults.colors(
                                    disabledCheckedThumbColor =
                                        MaterialTheme.colorScheme.surface
                                            .copy(1F)
                                            .compositeOver(MaterialTheme.colorScheme.surface),
                                    disabledCheckedTrackColor =
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(0.12F)
                                            .compositeOver(MaterialTheme.colorScheme.surface),
                                ),
                                onCheckedChange = { onToggle() },
                                modifier = Modifier
                                    .scale(0.7F)
                                    .width(40.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerProviderCardPlaceholder() {
    Placeholder(
        elevation = Elevations.LEVEL_3,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(40.dp + 12.dp)
    )
}

@Preview
@Composable
private fun TrackerProvidersBottomSheetPreview() {
    FlixclusiveTheme {
        Surface {
            TrackerProvidersBottomSheet(
                trackers = {
                    Async.Success(
                        List(20) {
                            TrackerProvider(
                                isTrackerEnabled = true,
                                isAuthenticated = it % 3 == 0,
                                metadata = DummyDataForPreview.getProviderMetadata(
                                    id = "provider_$it",
                                    name = "Provider ${it + 1}",
                                ),
                            )
                        }
                    )
                },
                onDismiss = {},
                onToggle = {},
                openProviderSettings = {},
            )
        }
    }
}

@Preview
@Composable
private fun TrackerCardPreview() {
    FlixclusiveTheme {
        Surface {
            LazyColumn {
                items(3) {
                    val item = remember {
                        DummyDataForPreview.getProviderMetadata(
                            id = it.toString(),
                            name = "Provider $it",
                            versionName = "1.0.$it",
                            versionCode = 100L + it,
                            status = ProviderStatus.entries[it % ProviderStatus.entries.size]
                        )
                    }

                    TrackerCard(
                        tracker = TrackerProvider(
                            isTrackerEnabled = true,
                            isAuthenticated = false,
                            metadata = item,
                        ),
                        enabled = { true },
                        onToggle = {},
                        openProviderSettings = {},
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
