package com.flixclusive.feature.mobile.user.edit.tweaks.data

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.strings.R
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.ui.common.dialog.ALERT_DIALOG_CORNER_SIZE
import com.flixclusive.core.ui.common.dialog.CharSequenceText
import com.flixclusive.core.ui.common.dialog.CustomBaseAlertDialog
import com.flixclusive.core.ui.common.util.IconResource
import com.flixclusive.core.ui.mobile.component.CustomCheckbox
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.feature.mobile.user.edit.Library
import com.flixclusive.feature.mobile.user.edit.tweaks.BaseProfileTweak
import com.flixclusive.feature.mobile.user.edit.tweaks.ProfileTweakUI
import com.flixclusive.feature.mobile.user.edit.tweaks.data.DataTweak.Companion.DeleteDialog
import kotlinx.coroutines.launch

internal class DataTweak(
    private val onClearSearchHistory: () -> Unit,
    private val onDeleteProfile: () -> Unit,
    private val onClearLibraries: (libraries: List<Library>) -> Unit,
) : BaseProfileTweak {
    @Composable
    override fun getLabel()
        = stringResource(R.string.data)

    override fun getTweaks(): List<ProfileTweakUI<*>> {
        return listOf(
            ProfileTweakUI.Button(
                label = UiText.StringResource(R.string.clear_search_history),
                description = UiText.StringResource(R.string.search_history_content_desc),
                icon = IconResource.fromDrawableResource(com.flixclusive.core.ui.common.R.drawable.search_outlined),
                needsConfirmation = true,
                onClick = onClearSearchHistory
            ),
            getDeleteDialog(),
            ProfileTweakUI.Button(
                label = UiText.StringResource(R.string.delete_profile),
                description = UiText.StringResource(R.string.delete_profile_content_desc),
                icon = IconResource.fromDrawableResource(com.flixclusive.core.ui.common.R.drawable.delete_person),
                needsConfirmation = true,
                onClick = onDeleteProfile
            ),
        )
    }

    private fun getDeleteDialog(): ProfileTweakUI.Dialog {
        return ProfileTweakUI.Dialog(
            label = UiText.StringResource(R.string.clear_library),
            description = UiText.StringResource(R.string.clear_library_content_desc),
            icon = IconResource.fromDrawableResource(com.flixclusive.core.ui.common.R.drawable.library_outline),
        ) { onDismiss ->
            DeleteDialog(
                onDismiss = onDismiss,
                onConfirm = onClearLibraries,
            )
        }
    }

    companion object {
        @Composable
        fun DeleteDialog(
            onConfirm: (List<Library>) -> Unit,
            onDismiss: () -> Unit,
        ) {
            val scope = rememberCoroutineScope()
            val buttonMinHeight = 50.dp
            val cornerSize = CornerSize(
                (ALERT_DIALOG_CORNER_SIZE * 2).dp
            )
            val shape = MaterialTheme.shapes.medium
            val buttonShape = shape.let {
                it.copy(
                    bottomStart = cornerSize,
                    bottomEnd = it.bottomEnd,
                )
            }

            val selectedLibraries = remember {
                mutableStateMapOf(
                    Library.Watchlist to false,
                    Library.WatchHistory to false,
                )
            }

            CustomBaseAlertDialog(
                onDismiss = onDismiss,
                action = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .padding(bottom = 10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val selected = AppDispatchers.withDefaultContext {
                                        selectedLibraries.mapNotNull { (key, checked) ->
                                            if (checked) key else null
                                        }
                                    }

                                    onConfirm(selected)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = buttonShape,
                            modifier = Modifier
                                .weight(1F)
                                .heightIn(min = buttonMinHeight)
                        ) {
                            Text(
                                text = stringResource(id = R.string.confirm),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(end = 2.dp)
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            ),
                            shape = buttonShape.copy(
                                bottomStart = shape.bottomStart,
                                bottomEnd = cornerSize,
                            ),
                            modifier = Modifier
                                .weight(1F)
                                .heightIn(min = buttonMinHeight)
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }
            ) {
                CharSequenceText(
                    text = stringResource(R.string.what_to_remove),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier
                        .padding(10.dp)
                )

                val availableLibraries by remember {
                    derivedStateOf {
                        selectedLibraries.keys
                            .toList()
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxWidth(0.9F)
                        .padding(vertical = 20.dp)
                ) {
                    items(availableLibraries) { library ->
                        val onCheckedChange = fun(state: Boolean) {
                            selectedLibraries[library] = state
                        }

                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .clickable {
                                    onCheckedChange(
                                        !selectedLibraries[library]!!
                                    )
                                }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp)
                            ) {
                                CustomCheckbox(
                                    checked = selectedLibraries[library]!!,
                                    onCheckedChange = onCheckedChange
                                )

                                CharSequenceText(
                                    text = library.name.asString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DataTweakBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface)
        ) {
            DeleteDialog(
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun DataTweakCompactLandscapePreview() {
    DataTweakBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun DataTweakMediumPortraitPreview() {
    DataTweakBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun DataTweakMediumLandscapePreview() {
    DataTweakBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun DataTweakExtendedPortraitPreview() {
    DataTweakBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun DataTweakExtendedLandscapePreview() {
    DataTweakBasePreview()
}
