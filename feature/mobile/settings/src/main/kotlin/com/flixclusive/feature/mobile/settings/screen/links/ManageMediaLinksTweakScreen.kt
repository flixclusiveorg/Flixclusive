package com.flixclusive.feature.mobile.settings.screen.links

//import androidx.compose.foundation.ExperimentalFoundationApi
//import androidx.compose.foundation.combinedClickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.FilterChip
//import androidx.compose.material3.HorizontalDivider
//import androidx.compose.material3.LinearProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedButton
//import androidx.compose.material3.SuggestionChip
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.ReadOnlyComposable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.style.TextDecoration
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.datastore.preferences.core.stringPreferencesKey
//import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.flixclusive.core.common.domain.Async
//import com.flixclusive.core.database.entity.provider.DBStream
//import com.flixclusive.core.database.entity.provider.DBSubtitle
//import com.flixclusive.core.datastore.model.FlixclusivePrefs
//import com.flixclusive.core.presentation.common.util.CustomClipboardManager
//import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
//import com.flixclusive.domain.provider.usecase.links.TestLinksProgress
//import com.flixclusive.feature.mobile.settings.Tweak
//import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
//import kotlinx.coroutines.flow.StateFlow
//import com.flixclusive.core.strings.R as LocaleR
//
//internal object ManageMediaLinksTweakScreen : BaseTweakScreen<FlixclusivePrefs> {
//    const val ROUTE = "manage_media_links"
//
//    override val isSubNavigation: Boolean = true
//    override val key = stringPreferencesKey(ROUTE)
//
//    override val preferencesAsState: StateFlow<FlixclusivePrefs>
//        get() = throw NotImplementedError("ManageMediaLinksTweakScreen does not manage preferences")
//
//    override fun onUpdatePreferences(transform: suspend (FlixclusivePrefs) -> FlixclusivePrefs) = Unit
//
//    @Composable
//    @ReadOnlyComposable
//    override fun getTitle(): String = ""
//
//    @Composable
//    @ReadOnlyComposable
//    override fun getDescription(): String = ""
//
//    @Composable
//    override fun getTweaks(): List<Tweak> = listOf()
//
//    @OptIn(ExperimentalFoundationApi::class)
//    @Composable
//    override fun Content() {
//        val viewModel = hiltViewModel<MediaLinksTweakViewModel>()
//        val selectedEntry by viewModel.selectedEntry.collectAsStateWithLifecycle()
//        val testState by viewModel.testState.collectAsStateWithLifecycle()
//        val clipboardManager = CustomClipboardManager.rememberClipboardManager()
//
//        if (selectedEntry == null) {
//            EmptyDataMessage(
//                modifier = Modifier.fillMaxSize(),
//                emojiHeader = "🫥",
//                title = stringResource(LocaleR.string.label_no_cached_media),
//            )
//            return
//        }
//
//        val entry = selectedEntry!!
//        var showDeadLinks by remember { mutableStateOf(false) }
//
//        Column(modifier = Modifier.fillMaxSize()) {
//            // Entry info header
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 12.dp, vertical = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(2.dp),
//            ) {
//                Text(
//                    text = entry.media.title,
//                    style = MaterialTheme.typography.titleMedium,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis,
//                )
//                Text(
//                    text = entry.cache.providerId,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                )
//            }
//
//            HorizontalDivider()
//
//            // Filter chips
//            LazyRow(
//                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//            ) {
//                item {
//                    FilterChip(
//                        selected = showDeadLinks,
//                        onClick = { showDeadLinks = !showDeadLinks },
//                        label = { Text(stringResource(LocaleR.string.show_dead_links)) },
//                    )
//                }
//            }
//
//            HorizontalDivider()
//
//            // Test progress bar
//            if (testState != null) {
//                TestProgressContent(testState = testState!!)
//                HorizontalDivider()
//            }
//
//            // Streams and subtitles list
//            val visibleStreams: List<DBStream> = remember(entry.streams, showDeadLinks) {
//                if (showDeadLinks) entry.streams else entry.streams.filter { it.isValid }
//            }
//            val visibleSubtitles: List<DBSubtitle> = remember(entry.subtitles, showDeadLinks) {
//                if (showDeadLinks) entry.subtitles else entry.subtitles.filter { !it.isDead }
//            }
//
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(1f),
//            ) {
//                if (visibleStreams.isNotEmpty()) {
//                    item {
//                        Text(
//                            text = stringResource(LocaleR.string.stream),
//                            style = MaterialTheme.typography.labelMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
//                        )
//                    }
//                    items(visibleStreams, key = { it.url }) { stream ->
//                        StreamItem(
//                            stream = stream,
//                            onLongClick = {
//                                clipboardManager.setText(stream.url)
//                            },
//                        )
//                        HorizontalDivider()
//                    }
//                }
//
//                if (visibleSubtitles.isNotEmpty()) {
//                    item {
//                        Text(
//                            text = stringResource(LocaleR.string.subtitle),
//                            style = MaterialTheme.typography.labelMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
//                        )
//                    }
//                    items(visibleSubtitles, key = { it.url }) { subtitle ->
//                        SubtitleItem(
//                            subtitle = subtitle,
//                            onLongClick = {
//                                clipboardManager.setText(subtitle.url)
//                            },
//                        )
//                        HorizontalDivider()
//                    }
//                }
//
//                if (visibleStreams.isEmpty() && visibleSubtitles.isEmpty()) {
//                    item {
//                        EmptyDataMessage(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(32.dp),
//                            emojiHeader = "🫥",
//                            title = stringResource(LocaleR.string.label_no_cached_media),
//                        )
//                    }
//                }
//            }
//
//            // Action row
//            HorizontalDivider()
//            ActionRow(
//                testState = testState,
//                onTestLinks = viewModel::onTestLinks,
//                onDeleteEntry = { viewModel.deleteEntry(entry.cache) },
//            )
//        }
//    }
//}
//
//@Composable
//private fun TestProgressContent(testState: Async<TestLinksProgress>) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 12.dp, vertical = 8.dp),
//    ) {
//        when (testState) {
//            is Async.Loading -> {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                ) {
//                    CircularProgressIndicator(modifier = Modifier.height(16.dp))
//                    Text(
//                        text = "Preparing…",
//                        style = MaterialTheme.typography.bodySmall,
//                    )
//                }
//            }
//
//            is Async.Success -> {
//                when (val progress = testState.data) {
//                    is TestLinksProgress.Testing -> {
//                        Column(
//                            modifier = Modifier.fillMaxWidth(),
//                            verticalArrangement = Arrangement.spacedBy(4.dp),
//                        ) {
//                            LinearProgressIndicator(
//                                progress = { progress.index.toFloat() / progress.total.toFloat() },
//                                modifier = Modifier.fillMaxWidth(),
//                            )
//                            Text(
//                                text = stringResource(
//                                    LocaleR.string.testing_link_format,
//                                    progress.index,
//                                    progress.total,
//                                ),
//                                style = MaterialTheme.typography.bodySmall,
//                            )
//                        }
//                    }
//
//                    is TestLinksProgress.Done -> {
//                        Text(
//                            text = stringResource(
//                                LocaleR.string.links_test_done_format,
//                                progress.aliveCount,
//                                progress.deadCount,
//                            ),
//                            style = MaterialTheme.typography.bodySmall,
//                        )
//                    }
//
//                    is TestLinksProgress.Error -> {
//                        Text(
//                            text = progress.cause.message ?: "Test failed",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.error,
//                        )
//                    }
//                }
//            }
//
//            is Async.Failure -> {
//                Text(
//                    text = testState.message.toString(),
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.error,
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//private fun StreamItem(
//    stream: DBStream,
//    onLongClick: () -> Unit,
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .combinedClickable(onLongClick = onLongClick, onClick = {})
//            .padding(horizontal = 12.dp, vertical = 8.dp),
//        verticalArrangement = Arrangement.spacedBy(4.dp),
//    ) {
//        Text(
//            text = stream.label,
//            style = MaterialTheme.typography.bodyMedium.let {
//                if (stream.isDead) it.copy(textDecoration = TextDecoration.LineThrough) else it
//            },
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis,
//        )
//        Text(
//            text = stream.url,
//            style = MaterialTheme.typography.bodySmall,
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis,
//        )
//        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
//            if (stream.expiresOn != null) {
//                SuggestionChip(
//                    onClick = {},
//                    label = { Text("Expires") },
//                )
//            }
//            if (stream.isThirdPartyGateway) {
//                SuggestionChip(
//                    onClick = {},
//                    label = {
//                        Text(stream.thirdPartyGatewayName ?: "3rd Party")
//                    },
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//private fun SubtitleItem(
//    subtitle: DBSubtitle,
//    onLongClick: () -> Unit,
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .combinedClickable(onLongClick = onLongClick, onClick = {})
//            .padding(horizontal = 12.dp, vertical = 8.dp),
//        verticalArrangement = Arrangement.spacedBy(2.dp),
//    ) {
//        Text(
//            text = subtitle.label,
//            style = MaterialTheme.typography.bodyMedium.let {
//                if (subtitle.isDead) it.copy(textDecoration = TextDecoration.LineThrough) else it
//            },
//        )
//        Text(
//            text = subtitle.subtitleSource,
//            style = MaterialTheme.typography.bodySmall,
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//        )
//        Text(
//            text = subtitle.url,
//            style = MaterialTheme.typography.bodySmall,
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis,
//        )
//    }
//}
//
//@Composable
//private fun ActionRow(
//    testState: Async<TestLinksProgress>?,
//    onTestLinks: () -> Unit,
//    onDeleteEntry: () -> Unit,
//) {
//    val isTesting = testState is Async.Loading ||
//        (testState is Async.Success && testState.data is TestLinksProgress.Testing)
//
//    Row(
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 12.dp, vertical = 8.dp),
//    ) {
//        OutlinedButton(
//            onClick = onTestLinks,
//            enabled = !isTesting,
//            modifier = Modifier.weight(1f),
//        ) {
//            Text(stringResource(LocaleR.string.test_links_label))
//        }
//
//        Button(
//            onClick = onDeleteEntry,
//            colors = ButtonDefaults.buttonColors(
//                containerColor = MaterialTheme.colorScheme.errorContainer,
//                contentColor = MaterialTheme.colorScheme.onErrorContainer,
//            ),
//        ) {
//            Text(stringResource(LocaleR.string.delete))
//        }
//    }
//}
