package com.flixclusive.feature.mobile.user.edit.tweaks.identity

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.common.util.IconResource
import com.flixclusive.feature.mobile.user.edit.tweaks.BaseProfileTweak
import com.flixclusive.feature.mobile.user.edit.tweaks.ProfileTweakUI
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal class IdentityTweak(
    private val initialName: String,
    private val onNameChange: (String) -> Unit,
    private val onSetupPin: () -> Unit,
) : BaseProfileTweak {
    @Composable
    override fun getLabel()
        = stringResource(LocaleR.string.identity)

    override fun getTweaks(): List<ProfileTweakUI<*>> {
        return listOf(
            ProfileTweakUI.TextField(
                initialValue = initialName,
                placeholder = UiText.StringResource(LocaleR.string.name),
                onValueChange = onNameChange
            ),
            ProfileTweakUI.Button(
                label = UiText.StringResource(LocaleR.string.pin),
                description = UiText.StringResource(LocaleR.string.pin_content_desc),
                icon = IconResource.fromDrawableResource(UiCommonR.drawable.pin_lock),
                onClick = onSetupPin
            ),
        )
    }
}