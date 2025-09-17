package com.flixclusive.feature.mobile.profiles

import com.flixclusive.core.navigation.navigator.AddProfileAction
import com.flixclusive.core.navigation.navigator.EditUserAction
import com.flixclusive.core.navigation.navigator.ExitAction
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.OpenPinScreenAction
import com.flixclusive.core.navigation.navigator.SelectAvatarAction
import com.flixclusive.core.navigation.navigator.StartHomeScreenAction

interface UserProfilesScreenNavigator :
    ExitAction,
    GoBackAction,
    StartHomeScreenAction,
    AddProfileAction,
    OpenPinScreenAction,
    SelectAvatarAction,
    EditUserAction
