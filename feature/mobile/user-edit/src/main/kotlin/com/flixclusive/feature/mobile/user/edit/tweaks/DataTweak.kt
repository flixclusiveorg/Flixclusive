package com.flixclusive.feature.mobile.user.edit.tweaks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.dialog.ALERT_DIALOG_ROUNDNESS_PERCENTAGE
import com.flixclusive.core.ui.common.dialog.CharSequenceText
import com.flixclusive.core.ui.common.dialog.CustomBaseAlertDialog
import com.flixclusive.feature.mobile.user.edit.ProfileTweak
import com.flixclusive.feature.mobile.user.edit.ProfileTweakUI
import com.flixclusive.feature.mobile.user.edit.tweaks.DataTweak.DeleteDialog
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object DataTweak : ProfileTweak {
    @Composable
    override fun getLabel()
        = stringResource(LocaleR.string.data)

    @Composable
    override fun getTweaks(): List<ProfileTweakUI<*>> {
        return listOf(
            ProfileTweakUI.Button(
                label = stringResource(LocaleR.string.clear_search_history),
                icon = painterResource(UiCommonR.drawable.search_outlined),
                onClick = { /*TODO*/ }
            ),

        )
    }

    @Composable
    private fun getDeleteDialog(): ProfileTweakUI.Dialog {
        return ProfileTweakUI.Dialog(
            label = stringResource(LocaleR.string.clear_library),
            icon = painterResource(UiCommonR.drawable.library_outline),
            onClick = { /*TODO*/ },
        ) {
            DeleteDialog(
                onDismiss = {},
                onConfirm = {},
            )
        }
    }

    @Composable
    fun DeleteDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        val buttonMinHeight = 50.dp
        val cornerSize = CornerSize(
            (ALERT_DIALOG_ROUNDNESS_PERCENTAGE * 2).dp
        )
        val buttonShape = MaterialTheme.shapes.medium.let {
            it.copy(
                bottomStart = cornerSize,
                bottomEnd = it.bottomEnd,
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
                            onConfirm()
                            onDismiss()
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
                            text = stringResource(id = LocaleR.string.confirm),
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
                            bottomEnd = cornerSize,
                        ),
                        modifier = Modifier
                            .weight(1F)
                            .heightIn(min = buttonMinHeight)
                    ) {
                        Text(
                            text = stringResource(LocaleR.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        ) {
            CharSequenceText(
                text = stringResource(LocaleR.string.what_to_remove),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                modifier = Modifier
                    .padding(10.dp)
            )
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