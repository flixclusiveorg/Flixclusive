package com.flixclusive.feature.mobile.user.edit

import com.flixclusive.core.navigation.navigator.ChooseProfileAction
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.OpenPinScreenAction
import com.flixclusive.core.navigation.navigator.SelectAvatarAction

interface UserEditScreenNavigator :
    OpenPinScreenAction,
    SelectAvatarAction,
    ChooseProfileAction,
    GoBackAction
