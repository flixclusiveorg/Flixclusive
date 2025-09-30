package com.flixclusive.feature.mobile.library.manage.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun LibraryOptionsBottomSheet(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val buttons = remember { getButtons(onEdit = onEdit, onDelete = onDelete) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.small.copy(
            bottomEnd = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp),
        ),
        dragHandle = { DragHandle() },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            buttons.forEach {
                TextButton(
                    onClick = it.action,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AdaptiveIcon(
                        painter = painterResource(it.drawableId),
                        contentDescription = stringResource(it.stringId),
                    )

                    Text(
                        text = stringResource(it.stringId),
                        style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                    )
                }
            }
        }
    }
}

private fun getButtons(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
): ImmutableList<ModifySheetItem> {
    return persistentListOf(
        ModifySheetItem(
            drawableId = UiCommonR.drawable.edit,
            stringId = LocaleR.string.edit,
            action = onEdit,
        ),
        ModifySheetItem(
            drawableId = UiCommonR.drawable.delete_outlined,
            stringId = LocaleR.string.delete,
            action = onDelete,
        ),
    )
}

@Immutable
private data class ModifySheetItem(
    @DrawableRes val drawableId: Int,
    @StringRes val stringId: Int,
    val action: () -> Unit,
)
