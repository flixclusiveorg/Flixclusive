package com.flixclusive.feature.mobile.settings.screen.root

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToChooseProfileScreen
import com.flixclusive.core.navigation.navigator.NavigateToEditUserScreen
import com.flixclusive.core.navigation.navigator.NavigateToMediaPreviewBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToSubSettingsScreen

interface NavigatorSettingsScreen :
    NavigateBack,
    NavigateToChooseProfileScreen,
    NavigateToEditUserScreen,
    NavigateToMediaPreviewBottomSheet,
    NavigateToSubSettingsScreen {
    fun navigateToRepositoryManagerScreen()

    fun navigateToProviderManagerScreen()

    fun navigateToUrl(url: String)
}
