package com.flixclusive.feature.mobile.settings.screen.links.manage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.database.entity.provider.DBMediaLink
import com.flixclusive.core.database.entity.provider.DBStream
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToMediaLinksBottomSheet
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.domain.provider.usecase.links.TestLinksProgress
import com.flixclusive.feature.mobile.settings.R
import com.flixclusive.feature.mobile.settings.screen.links.manage.LinkUtil.toRelativeTime
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.tv.Episode
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.flow.collectLatest
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

data class ManageMediaLinksTweakScreenArgs(
    val media: MediaMetadata,
    val episode: Episode? = null,
)

interface NavigatorManageMediaLinksTweakScreen :
    NavigateBack,
    NavigateToMediaLinksBottomSheet

@Destination<ExternalModuleGraph>(
    navArgs = ManageMediaLinksTweakScreenArgs::class
)
@Composable
internal fun ManageMediaLinksTweakScreen(
    navigator: NavigatorManageMediaLinksTweakScreen,
    viewModel: ManageMediaLinksTweakViewModel = hiltViewModel()
) {
    val links by viewModel.links.collectAsStateWithLifecycle()
    val providerFilters by viewModel.providerFilters.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val selectedLinks by viewModel.selectedLinks.collectAsStateWithLifecycle()
    val testProgress by viewModel.testProgress.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is ManageMediaLinksTweakEvent.PlayLink -> {
                    val media = viewModel.args.media
                    if (media !is PartialMedia) {
                        navigator.showPlayerSplashScreen(
                            media = media,
                            episode = viewModel.args.episode,
                            initialStreamUrl = event.link.url,
                            initialCacheId = event.link.parentId,
                            initialHeaders = event.link.customHeaders
                        )
                    }
                }
            }
        }
    }

    ManageMediaLinksTweakScreenContent(
        links = links,
        providerFilters = providerFilters,
        typeFilter = typeFilter,
        selectedLinks = { selectedLinks },
        testProgress = testProgress,
        onNavigateBack = navigator::navigateBack,
        onTypeFilterChange = viewModel::onUpdateTypeFilter,
        onProviderFilterChange = { viewModel.onUpdateProviderFilter(it.provider.id) },
        onToggleSelect = viewModel::onToggleSelect,
        onSelectAll = viewModel::onSelectAll,
        onClearSelection = viewModel::onClearSelection,
        onTestLinks = viewModel::onTestLinks,
        onDeleteLinks = viewModel::onDeleteLinks,
        onResetLinks = viewModel::onResetLinks,
        onPlayLink = viewModel::onPlayLink
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ManageMediaLinksTweakScreenContent(
    links: Async<List<DBMediaLink>>,
    providerFilters: List<ProviderFilterState>,
    typeFilter: LinkType,
    selectedLinks: () -> Set<DBMediaLink>,
    testProgress: TestLinksProgress?,
    onNavigateBack: () -> Unit,
    onTypeFilterChange: (LinkType) -> Unit,
    onProviderFilterChange: (ProviderFilterState) -> Unit,
    onToggleSelect: (DBMediaLink) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onTestLinks: (List<DBMediaLink>) -> Unit,
    onDeleteLinks: (List<DBMediaLink>) -> Unit,
    onResetLinks: (List<DBMediaLink>) -> Unit,
    onPlayLink: (DBMediaLink) -> Unit,
) {
    val totalCount = (links as? Async.Success)?.data?.size ?: 0

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(LocaleR.string.label_cached_links),
                onNavigate = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Bar
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (links is Async.Loading) {
                    repeat(4) {
                        Placeholder(
                            modifier = Modifier
                                .size(width = 80.dp, height = 32.dp)
                                .clip(CircleShape)
                        )
                    }
                } else {
                    LinkType.entries.forEach { type ->
                        val isSelected = typeFilter == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTypeFilterChange(type) },
                            label = { Text(type.name) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) {
                                null
                            } else {
                                FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false
                                )
                            }
                        )
                    }

                    providerFilters.forEach { filter ->
                        FilterChip(
                            selected = filter.selected,
                            onClick = { onProviderFilterChange(filter) },
                            label = { Text(filter.provider.name) },
                            shape = CircleShape
                        )
                    }
                }
            }

            // Selection & Mass Actions
            AnimatedVisibility(
                visible = totalCount > 0,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = selectedLinks().isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Checkbox(
                            checked = selectedLinks().size == totalCount,
                            onCheckedChange = { if (it) onSelectAll() else onClearSelection() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                        )
                    }

                    AnimatedContent(
                        targetState = selectedLinks().size,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically { height -> height } + fadeIn()) togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            } else {
                                (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                        slideOutVertically { height -> height } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        },
                        label = "SelectionCount"
                    ) { count ->
                        Text(
                            text = if (count > 0) {
                                stringResource(R.string.count_selection_format, count)
                            } else {
                                pluralStringResource(
                                    LocaleR.plurals.number_of_items_format,
                                    totalCount,
                                    totalCount
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    AnimatedVisibility(
                        visible = selectedLinks().isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MassActionButton(
                                text = stringResource(LocaleR.string.test_links_label),
                                painter = painterResource(UiCommonR.drawable.round_refresh_24),
                                onClick = { onTestLinks(emptyList()) }
                            )
                            MassActionButton(
                                text = stringResource(LocaleR.string.reset),
                                painter = painterResource(UiCommonR.drawable.round_close_24),
                                onClick = { onResetLinks(emptyList()) },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // Test Progress
            if (testProgress != null && testProgress !is TestLinksProgress.Done) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    when (testProgress) {
                        is TestLinksProgress.Testing -> {
                            Column {
                                LinearProgressIndicator(
                                    progress = { testProgress.index.toFloat() / testProgress.total.toFloat() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = stringResource(
                                        LocaleR.string.testing_link_format,
                                        testProgress.index,
                                        testProgress.total
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        is TestLinksProgress.Error -> {
                            Text(
                                text = testProgress.cause.message ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // Links List
            AsyncAnimatedContent(
                targetState = links,
                loadingContent = {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            LinkCardPlaceholder()
                        }
                    }
                },
                errorContent = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = it.message.toString())
                    }
                }
            ) { items ->
                if (items().isEmpty()) {
                    EmptyDataMessage(
                        modifier = Modifier.fillMaxSize(),
                        emojiHeader = "🫥",
                        title = stringResource(LocaleR.string.label_no_cached_media)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items(), key = { it.url + it.parentId }) { link ->
                            LinkCard(
                                link = link,
                                isSelected = { link in selectedLinks() },
                                onToggleSelect = { onToggleSelect(link) },
                                onPlay = { onPlayLink(link) },
                                onDelete = { onDeleteLinks(listOf(link)) },
                                onReset = { onResetLinks(listOf(link)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MassActionButton(
    text: String,
    painter: androidx.compose.ui.graphics.painter.Painter,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = if (containerColor ==
            MaterialTheme.colorScheme.onSurface
        ) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painter, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LinkCard(
    link: DBMediaLink,
    isSelected: () -> Boolean,
    onToggleSelect: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit,
) {
    val resources = LocalContext.current.resources
    val isUntested = link.updatedAt == link.createdAt

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onToggleSelect
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected()) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary
            )
        } else {
            null
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                val icon =
                    if (link is DBStream) UiCommonR.drawable.play_outline_circle
                    else UiCommonR.drawable.outline_subtitles_24
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )

                    link.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(
                    isDead = link.isDead,
                    isUntested = isUntested
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Relative Time
                Text(
                    text = link.updatedAt.toRelativeTime(resources),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painterResource(UiCommonR.drawable.round_refresh_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onPlay, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painterResource(UiCommonR.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painterResource(UiCommonR.drawable.delete),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkCardPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Placeholder(modifier = Modifier.size(20.dp), shape = CircleShape)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Placeholder(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Placeholder(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
                }
                Placeholder(modifier = Modifier.size(8.dp), shape = CircleShape)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Placeholder(modifier = Modifier.size(width = 60.dp, height = 16.dp))
                Spacer(modifier = Modifier.weight(1f))
                repeat(3) {
                    Placeholder(modifier = Modifier.size(32.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isDead: Boolean, isUntested: Boolean) {
    if (isUntested) return

    val color = if (isDead) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Preview
@Composable
private fun LinkCardPreview() {
    FlixclusiveTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkCard(
                    link = PreviewData.getStream(),
                    isSelected = { false },
                    onToggleSelect = {},
                    onPlay = {},
                    onDelete = {},
                    onReset = {}
                )
                LinkCard(
                    link = PreviewData.getStream().copy(isDead = true),
                    isSelected = { true },
                    onToggleSelect = {},
                    onPlay = {},
                    onDelete = {},
                    onReset = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun ManageMediaLinksTweakScreenPreview() {
    val dummyStreams = List(5) { i ->
        DBStream(
            url = "https://example.com/video_$i.mkv",
            parentId = "provider-1",
            label = "Example Stream $i",
            description = "1080p • 2.3 GB",
            isDead = i % 2 == 0,
            createdAt = java.util.Date(),
            updatedAt = java.util.Date()
        )
    }

    FlixclusiveTheme {
        Surface {
            ManageMediaLinksTweakScreenContent(
                links = Async.Success(dummyStreams),
                providerFilters = listOf(
                    ProviderFilterState(DummyDataForPreview.getProviderMetadata(), true),
                    ProviderFilterState(DummyDataForPreview.getProviderMetadata().copy(name = "Provider 2"), false)
                ),
                typeFilter = LinkType.All,
                selectedLinks = { emptySet() },
                testProgress = null,
                onNavigateBack = {},
                onTypeFilterChange = {},
                onProviderFilterChange = {},
                onToggleSelect = {},
                onSelectAll = {},
                onClearSelection = {},
                onTestLinks = {},
                onDeleteLinks = {},
                onResetLinks = {},
                onPlayLink = {}
            )
        }
    }
}
