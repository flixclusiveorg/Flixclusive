package com.flixclusive.feature.mobile.settings.screen.github

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.util.common.GithubConstant.GITHUB_REPOSITORY_URL
import com.flixclusive.feature.mobile.settings.screen.BaseTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.root.SettingsScreenNavigator
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object FeatureRequestTweakNavigation : BaseTweakNavigation {
    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.feature_request)

    @Composable
    override fun getIconPainter(): Painter
        = painterResource(UiCommonR.drawable.feature_request)

    override fun onClick(navigator: SettingsScreenNavigator) {
        navigator.openLink("$GITHUB_REPOSITORY_URL/issues/new?assignees=&labels=enhancement&projects=&template=request_feature.yml")
    }
}
