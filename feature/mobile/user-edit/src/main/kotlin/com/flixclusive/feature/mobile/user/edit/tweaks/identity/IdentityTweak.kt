package com.flixclusive.feature.mobile.user.edit.tweaks.identity

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.util.IconResource
import com.flixclusive.feature.mobile.user.edit.tweaks.BaseProfileTweak
import com.flixclusive.feature.mobile.user.edit.tweaks.ProfileTweakUI
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal class IdentityTweak(
    private val initialName: String,
    private val userHasPin: Boolean,
    private val onNameChange: (String) -> Unit,
    private val onOpenPinScreen: (isRemoving: Boolean) -> Unit,
) : BaseProfileTweak {
    @Composable
    override fun getLabel() = stringResource(LocaleR.string.identity)

    override fun getTweaks(): List<ProfileTweakUI<*>> {
        return listOf(
            ProfileTweakUI.TextField(
                initialValue = initialName,
                placeholder = UiText.StringResource(LocaleR.string.name),
                onValueChange = onNameChange,
            ),
            ProfileTweakUI.Button(
                label = UiText.StringResource(LocaleR.string.pin),
                description = if (userHasPin) {
                    UiText.StringResource(LocaleR.string.pin_remove_content_desc)
                } else {
                    UiText.StringResource(
                        LocaleR.string.pin_content_desc,
                    )
                },
                icon = IconResource.from(UiCommonR.drawable.pin_lock),
                onClick = { onOpenPinScreen(userHasPin) },
            ),
        )
    }
}
