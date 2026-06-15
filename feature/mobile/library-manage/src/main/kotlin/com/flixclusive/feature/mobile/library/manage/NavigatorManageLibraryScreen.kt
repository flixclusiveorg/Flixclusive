package com.flixclusive.feature.mobile.library.manage

import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToProviderSettingsScreen
import com.flixclusive.model.provider.ProviderMetadata

interface NavigatorManageLibraryScreen :
    NavigateBack,
    NavigateToProviderSettingsScreen {
    fun navigateToLibraryDetailsScreen(list: LibraryList, tracker: ProviderMetadata? = null)
}
