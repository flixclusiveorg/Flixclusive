package com.flixclusive.feature.mobile.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.feature.mobile.about.component.BodyContent
import com.flixclusive.feature.mobile.about.component.Header
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR

@Destination
@Composable
internal fun AboutScreen(
    navigator: GoBackAction,
) {
    val viewModel = hiltViewModel<AboutScreenViewModel>()
    val isOnPreRelease by viewModel.isOnPreRelease.collectAsStateWithLifecycle()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(rememberScrollState()),
    ) {
        CommonTopBar(
            headerTitle = stringResource(id = LocaleR.string.about),
            onNavigationIconClick = navigator::goBack
        )

        viewModel.currentAppBuild?.run {
            Header(
                appName = applicationName,
                versionName = versionName,
                commitVersion = commitVersion,
                isInDebugMode = debug,
                isOnPreRelease = isOnPreRelease
            )
        }

        BodyContent()
    }
}