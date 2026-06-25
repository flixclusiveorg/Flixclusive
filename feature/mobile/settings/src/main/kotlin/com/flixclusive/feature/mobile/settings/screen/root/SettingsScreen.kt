package com.flixclusive.feature.mobile.settings.screen.root

import android.annotation.SuppressLint
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.navigation.settings.SubSettingsNavItem
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.model.media.MediaMetadata
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph

@Suppress("ktlint:compose:vm-forwarding-check")
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Destination<ExternalModuleGraph>
@Composable
internal fun SettingsScreen(
    navigator: NavigatorSettingsScreen,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    ListContent(
        buildConfig = viewModel.buildConfig,
        currentUser = { currentUser },
        navigator = navigator,
    )
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun PhonePreview() {
    FlixclusiveTheme {
        Surface {
            SettingsScreen(
                navigator = object : NavigatorSettingsScreen {
                    override fun navigateToProviderManagerScreen() = Unit

                    override fun navigateToRepositoryManagerScreen() = Unit

                    override fun navigateToUrl(url: String) = Unit

                    override fun navigateBack() = Unit

                    override fun navigateToUserProfilesScreen(shouldPopBackStack: Boolean) = Unit

                    override fun navigateToEditUserScreen(userId: String) = Unit

                    override fun showMediaPreviewBottomSheet(media: MediaMetadata) = Unit

                    override fun navigateToSubSettingsScreen(route: SubSettingsNavItem) = Unit
                },
            )
        }
    }
}
