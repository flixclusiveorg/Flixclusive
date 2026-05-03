package com.flixclusive.feature.mobile.library.manage

import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.ViewProviderSettingsAction
import com.flixclusive.model.provider.ProviderMetadata

interface ManageLibraryScreenNavigator : GoBackAction, ViewProviderSettingsAction {
    fun openLibraryDetails(list: LibraryList, tracker: ProviderMetadata? = null)
}
