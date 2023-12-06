package com.flixclusive.presentation.mobile.utils

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object DragAndDropUtils {
    fun Modifier.dragGestureHandler(
        scope: CoroutineScope,
        itemListDragAndDropState: ItemListDragAndDropState,
        overscrollJob: MutableState<Job?>,
        feedbackLongPress: () -> Unit,
    ): Modifier = pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDrag = { change, offset ->
                change.consume()
                itemListDragAndDropState.onDrag(offset)
                handleOverscrollJob(overscrollJob, scope, itemListDragAndDropState)
            },
            onDragStart = { offset ->
                feedbackLongPress()
                itemListDragAndDropState.onDragStart(offset)
            },
            onDragEnd = { itemListDragAndDropState.onDragInterrupted() },
            onDragCancel = { itemListDragAndDropState.onDragInterrupted() }
        )
    }

    private fun handleOverscrollJob(
        overscrollJob: MutableState<Job?>,
        scope: CoroutineScope,
        itemListDragAndDropState: ItemListDragAndDropState,
    ) {
        if (overscrollJob.value?.isActive == true) return
        val overscrollOffset = itemListDragAndDropState.checkForOverScroll()
        if (overscrollOffset != 0f) {
            overscrollJob.value = scope.launch {
                itemListDragAndDropState.getLazyListState().scrollBy(overscrollOffset)
            }
        } else {
            overscrollJob.value?.cancel()
        }
    }
}