package com.flixclusive.feature.mobile.settings.screen.github

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.navigation.navigator.SettingsScreenNavigator
import com.flixclusive.core.util.common.GithubConstant.GITHUB_REPOSITORY_URL
import com.flixclusive.feature.mobile.settings.screen.BaseTweakNavigation
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object IssueBugTweakNavigation : BaseTweakNavigation {
    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.issue_a_bug)

    @Composable
    override fun getIconPainter(): Painter
        = painterResource(UiCommonR.drawable.test_thin)

    override fun onClick(navigator: SettingsScreenNavigator) {
        navigator.openLink("$GITHUB_REPOSITORY_URL/issues/new?assignees=&labels=bug&projects=&template=report_issue.yml")
    }
}