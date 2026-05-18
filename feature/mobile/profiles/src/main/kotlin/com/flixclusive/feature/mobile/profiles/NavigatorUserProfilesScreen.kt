package com.flixclusive.feature.mobile.profiles

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToAddProfileScreen
import com.flixclusive.core.navigation.navigator.NavigateToEditUserScreen
import com.flixclusive.core.navigation.navigator.NavigateToHomeScreen
import com.flixclusive.core.navigation.navigator.NavigateToOpenPinScreen
import com.flixclusive.core.navigation.navigator.NavigateToSelectAvatarScreen
import com.flixclusive.core.navigation.navigator.NavigatorExitApp

interface NavigatorUserProfilesScreen :
    NavigatorExitApp,
    NavigateBack,
    NavigateToHomeScreen,
    NavigateToAddProfileScreen,
    NavigateToOpenPinScreen,
    NavigateToSelectAvatarScreen,
    NavigateToEditUserScreen
