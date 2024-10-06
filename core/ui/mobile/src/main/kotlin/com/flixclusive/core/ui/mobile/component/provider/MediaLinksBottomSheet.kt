package com.flixclusive.core.ui.mobile.component.provider

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientLinearProgressIndicator
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.core.ui.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.EmptyDataMessage
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flixclusive.core.ui.mobile.component.getDefaultSegmentedButtonColors
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.MediaLink.Companion.getOrNull
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLinksBottomSheet(
    state: MediaLinkResourceState,
    streams: List<Stream>,
    subtitles: List<Subtitle>,
    onDismiss: () -> Unit,
    onLinkClick: (MediaLink) -> Unit,
    onSkipLoading: () -> Unit
) {
    val areSubtitlesShown = remember { mutableStateOf(false) }
    val filteredLinks by rememberUpdatedState(newValue = if (areSubtitlesShown.value) subtitles else streams)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.small.copy(
            bottomEnd = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp)
        ),
        dragHandle = { DragHandle() }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (state.isLoading) {
                item {
                    ProgressHeader(
                        state = state,
                        streams = streams,
                        onSkipLoading = onSkipLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = !state.isIdle && (streams.isNotEmpty() || subtitles.isNotEmpty()),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FilterSegmentedButtons(
                        areSubtitlesShown = areSubtitlesShown,
                        state = state,
                        streams = streams,
                        subtitles = subtitles
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible =  filteredLinks.isEmpty() && state.isError,
                    enter = scaleIn() + fadeIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    ErrorMessage(
                        state = state,
                        modifier = Modifier
                            .padding(vertical = 20.dp)
                            .fillMaxSize()
                    )
                }
            }

            if (filteredLinks.isNotEmpty()) {
                item(key = 4) {
                    Spacer(
                        modifier = Modifier
                            .size(10.dp)
                            .animateItem()
                    )
                }

                items(
                    filteredLinks,
                    key = { it.hashCode() }
                ) {
                    MediaLinkItem(
                        modifier = Modifier.animateItem(),
                        link = it,
                        onClick = { onLinkClick(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DragHandle(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(top = 22.dp, bottom = 5.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2F),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            Modifier.size(
                width = 32.dp,
                height = 4.dp
            )
        )
    }
}

@Composable
private fun FilterSegmentedButtons(
    modifier: Modifier = Modifier,
    areSubtitlesShown: MutableState<Boolean>,
    state: MediaLinkResourceState,
    streams: List<Stream>,
    subtitles: List<Subtitle>,
) {
    val noCorner = CornerSize(0)
    val hasCorner = CornerSize(4.dp)
    val colors = getDefaultSegmentedButtonColors()

    val streamButtonCorner = if (subtitles.isEmpty()) hasCorner else noCorner

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedButton(
            selected = !areSubtitlesShown.value,
            onClick = { areSubtitlesShown.value = false },
            colors = colors,
            border = SegmentedButtonDefaults.borderStroke(
                color = colors.activeBorderColor.copy(alpha = 0.5F),
                width = 1.2.dp
            ),
            shape = MaterialTheme.shapes.extraSmall.copy(
                topEnd = streamButtonCorner,
                bottomEnd = streamButtonCorner
            ),
            label = {
                Text(
                    text = stringResource(id = LocaleR.string.stream),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )

        if (subtitles.isNotEmpty()) {
            SegmentedButton(
                selected = areSubtitlesShown.value,
                onClick = { areSubtitlesShown.value = true },
                shape = MaterialTheme.shapes.extraSmall.copy(
                    topStart = noCorner,
                    bottomStart = noCorner
                ),
                colors = colors,
                border = SegmentedButtonDefaults.borderStroke(
                    color = colors.activeBorderColor.copy(alpha = 0.5F),
                    width = 1.2.dp
                ),
                label = {
                    Text(
                        text = stringResource(id = LocaleR.string.subtitle),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

@Composable
private fun ProgressHeader(
    modifier: Modifier = Modifier,
    state: MediaLinkResourceState,
    streams: List<Stream>,
    onSkipLoading: () -> Unit,
) {
    val canSkipLoading by remember {
        derivedStateOf {
            state.isLoading && streams.isNotEmpty()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = state,
            label = "",
            transitionSpec = {
                if (targetState > initialState) {
                    fadeIn() + slideInHorizontally { it } togetherWith
                            fadeOut() + slideOutHorizontally { -it }
                } else {
                    fadeIn() + slideInHorizontally { -it } + fadeIn() togetherWith
                            fadeOut() + slideOutHorizontally { it }
                }.using(
                    SizeTransform(clip = false)
                )
            },
        ) {
            Text(
                text = it.message.asString().trim(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp
                )
            )
        }

        AnimatedVisibility(
            visible = state.isLoading,
            enter = fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            GradientLinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.7F),
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                )
            )
        }

        if (canSkipLoading) {
            ElevatedButton(
                onClick = onSkipLoading,
                shape = MaterialTheme.shapes.extraSmall,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .height(30.dp)
            ) {
                Text(
                    text = stringResource(id = LocaleR.string.skip_loading_message),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    modifier: Modifier = Modifier,
    state: MediaLinkResourceState
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            label = "",
            targetState = state,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = scaleIn() + fadeIn(),
                    initialContentExit = scaleOut() + fadeOut(),
                )
            }
        ) {
            val title = when {
                !it.isUnavailable && !it.isIdle -> stringResource(id = LocaleR.string.something_went_wrong)
                else -> stringResource(id = LocaleR.string.empty_data_default_label)
            }

            val description = when {
                !it.isIdle -> it.message.asString()
                else -> stringResource(id = LocaleR.string.empty_data_default_sub_label)
            }

            EmptyDataMessage(
                title = title,
                description = description,
                icon = if (it.isUnavailable || it.isIdle) null
                else {
                    {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.round_error_outline_24),
                            tint = MaterialTheme.colorScheme.error.onMediumEmphasis(),
                            contentDescription = stringResource(id = LocaleR.string.error_icon_content_desc),
                            modifier = Modifier.size(60.dp),
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaLinkItem(
    modifier: Modifier = Modifier,
    link: MediaLink,
    onClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = rememberClipboardManager()
    val trustedFlag = remember {
        link.flags?.getOrNull(Flag.Trusted::class)
    }
    val hasTrustedFlag = trustedFlag != null

    val clickLink = {
        when {
            hasTrustedFlag -> uriHandler.openUri(uri = link.url)
            else -> onClick.invoke()
        }
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                shape = MaterialTheme.shapes.extraSmall
            )
            .combinedClickable(
                onClick = clickLink,
                onLongClick = {
                    clipboardManager.setText(
                        """
                        Stream name: ${link.name}
                        Stream link: ${link.url}
                        Stream headers: ${link.customHeaders}
                        """.trimIndent()
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 60.dp)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasTrustedFlag) {
                ImageWithSmallPlaceholder(
                    urlImage = trustedFlag!!.logo,
                    placeholderId = UiCommonR.drawable.provider_logo,
                    contentDescId = LocaleR.string.provider_icon_content_desc,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .size(50.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1F),
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = link.name.trim(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                )

                Text(
                    text = link.description ?: link.url,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (link.description == null) 1 else Int.MAX_VALUE,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = LocalContentColor.current.onMediumEmphasis(),
                        fontSize = 12.sp,
                    )
                )
            }
        }
    }
}

@Suppress("SpellCheckingInspection")
@Preview
@Composable
private fun MediaLinksBottomSheetPreview() {
    val links = remember { mutableStateListOf<MediaLink>() }
//    val links = MutableList<MediaLink>(2) {
//        val randomBool = Random.nextBoolean()
//
//        if (it % 2 == 0) {
//            Stream(
//                name = "Server $it",
//                description = if (randomBool) "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Aliquam erat volutpat. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae;" else null,
//                url = "https://www.google.com",
//                flags = if (randomBool) setOf(
//                    Flag.Trusted(
//                        url = "https://www.google.com",
//                        name = "Netflix",
//                        description = "Description",
//                        logo = "https://media.themoviedb.org/t/p/original/9BgaNQRMDvVlji1JBZi6tcfxpKx.jpg"
//                    )
//                ) else null
//            )
//        } else {
//            Subtitle(
//                language = "Sub $it",
//                url = "https://www.google.com"
//            )
//        }
//    }
    var state by remember { mutableStateOf<MediaLinkResourceState>(MediaLinkResourceState.Idle) }

    LaunchedEffect(true) {
        var itemCount = 0
        val delayTime = 400L

        delay(delayTime)
        state = MediaLinkResourceState.Fetching()
        delay(delayTime)
        state = MediaLinkResourceState.Extracting()
        while (itemCount < 10) {
            val randomBool = Random.nextBoolean()
            val link = if (itemCount % 2 == 0) {
                Stream(
                    name = "Server $itemCount",
                    description = if (randomBool) "Lorem ipsum dolor sit amet, consectetur;" else null,
                    url = "https://www.google.com",
                    flags = if (randomBool) setOf(
                        Flag.Trusted(
                            url = "https://www.google.com",
                            name = "Netflix",
                            description = "Description",
                            logo = "https://media.themoviedb.org/t/p/original/9BgaNQRMDvVlji1JBZi6tcfxpKx.jpg"
                        )
                    ) else null
                )
            } else {
                Subtitle(
                    language = "Sub $itemCount",
                    url = "https://www.google.com"
                )
            }

            delay(delayTime)
            links.add(link)
            itemCount++
        }
        delay(delayTime)
        state = MediaLinkResourceState.Success
//        delay(delayTime)
//        state = MediaLinkResourceState.Unavailable()
//        delay(delayTime * 3L)
//        state = MediaLinkResourceState.Error()
    }

    FlixclusiveTheme {
        Surface {
            MediaLinksBottomSheet(
                state = state,
                streams = links.filterIsInstance<Stream>(),
                subtitles = links.filterIsInstance<Subtitle>(),
                onDismiss = {},
                onSkipLoading = {},
                onLinkClick = {}
            )
        }
    }
}