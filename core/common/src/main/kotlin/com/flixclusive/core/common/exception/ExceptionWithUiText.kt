package com.flixclusive.core.common.exception

import com.flixclusive.core.common.locale.UiText

class ExceptionWithUiText(
    val uiText: UiText? = null,
    override val cause: Throwable? = null,
    override val message: String? = null,
) : Throwable()
