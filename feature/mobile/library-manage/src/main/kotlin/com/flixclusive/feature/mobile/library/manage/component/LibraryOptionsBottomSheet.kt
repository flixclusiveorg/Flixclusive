package com.flixclusive.feature.mobile.library.manage.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            buttons.forEach {
                ItemButton(
                    painter = painterResource(it.drawableId),
                    label = stringResource(it.stringId),
                    onClick = it.action
                )
            }
        }
    }
}


@Composable
private fun ItemButton(
    painter: Painter,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .minimumInteractiveComponentSize()
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveIcon(
            painter = painter,
            contentDescription = null
        )

        Text(
            text = label,
            style =
            getAdaptiveTextStyle(
                style = AdaptiveTextStyle.Emphasized,
                style = TypographyStyle.Label,
                increaseBy = 2.sp,
            ),
        )
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
            action = onEdit
        ),
        ModifySheetItem(
            drawableId = UiCommonR.drawable.delete_outlined,
            stringId = LocaleR.string.delete,
            action = onDelete
        ),
    )
}

@Immutable
private data class ModifySheetItem(
    @DrawableRes val drawableId: Int,
    @StringRes val stringId: Int,
    val action: () -> Unit,
)
