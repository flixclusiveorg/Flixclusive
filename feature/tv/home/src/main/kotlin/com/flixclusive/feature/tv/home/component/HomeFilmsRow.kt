package com.flixclusive.feature.tv.home.component

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.tv.component.MediaCard
import com.flixclusive.core.ui.tv.component.MediaCardHeight
import com.flixclusive.core.ui.tv.component.MediaPadding
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.createInitialFocusRestorerModifiers
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.core.ui.tv.util.getLocalDrawerWidth
import com.flixclusive.core.ui.tv.util.shouldPaginate
import com.flixclusive.core.common.pagination.PagingState
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.domain.home.PaginationStateInfo
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.provider.Catalog

internal const val HOME_FOCUS_KEY_FORMAT = "row=%d, column=%d"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun HomeMediasRow(
    modifier: Modifier = Modifier,
    catalogItem: Catalog,
    paginationState: PaginationStateInfo,
    medias: List<MediaMetadata>,
    rowIndex: Int,
    onMediaClick: (media: MediaMetadata) -> Unit,
    onFocusedMediaChange: (media: MediaMetadata) -> Unit,
    paginate: (page: Int) -> Unit,
) {
    val focusRestorers = createInitialFocusRestorerModifiers()

    val listState = rememberTvLazyListState()

    val shouldStartPaginate by remember {
        derivedStateOf {
            listState.shouldPaginate()
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        safeCall {
            if (
                medias.isNotEmpty() &&
                listState.firstVisibleItemIndex % medias.size == 1 &&
                listState.firstVisibleItemIndex > medias.size &&
                !paginationState.canPaginate
            ) {
                listState.scrollToItem(0)
            }
        }
    }

    LaunchedEffect(shouldStartPaginate) {
        if (
            shouldStartPaginate && paginationState.canPaginate &&
            (
                paginationState.pagingState == com.flixclusive.core.common.pagination.PagingState.IDLE ||
                    paginationState.pagingState == com.flixclusive.core.common.pagination.PagingState.ERROR ||
                    medias.isEmpty()
            )
        ) {
            paginate(paginationState.currentPage)
        }
    }

    Column(
        modifier = Modifier
            .heightIn(min = MediaPadding.bottom + 18.dp + MediaCardHeight)
    ) {
        Text(
            text = catalogItem.name,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),
            modifier = Modifier
                .padding(start = LabelStartPadding.start + getLocalDrawerWidth())
                .padding(
                    bottom = MediaPadding.bottom,
                    top = 18.dp
                )
        )

        TvLazyRow(
            modifier = Modifier
                .focusGroup()
                .then(focusRestorers.parentModifier),
            pivotOffsets = PivotOffsets(parentFraction = 0.07F),
            state = listState,
            contentPadding = PaddingValues(
                start = LabelStartPadding.start + getLocalDrawerWidth()
            )
        ) {
            items(
                count = if (paginationState.canPaginate) medias.size else Int.MAX_VALUE
            ) {
                val columnIndex = it % medias.size
                val media = medias[columnIndex]

                val key = String.format(HOME_FOCUS_KEY_FORMAT, rowIndex, columnIndex)

                MediaCard(
                    modifier = Modifier
                        .ifElse(
                            condition = it == 0,
                            ifTrueModifier = focusRestorers.childModifier
                        ).focusOnMount(
                            itemKey = key,
                            onFocus = {
                                onFocusedMediaChange(media)
                            }
                        ),
                    media = media,
                    onClick = onMediaClick
                )
            }
        }
    }
}
