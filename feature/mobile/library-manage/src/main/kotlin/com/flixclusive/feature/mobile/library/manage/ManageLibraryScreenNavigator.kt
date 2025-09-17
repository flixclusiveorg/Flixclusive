package com.flixclusive.feature.mobile.library.manage

import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.navigation.navigator.GoBackAction

interface ManageLibraryScreenNavigator : GoBackAction {
    fun openLibraryDetails(list: LibraryList)
}
