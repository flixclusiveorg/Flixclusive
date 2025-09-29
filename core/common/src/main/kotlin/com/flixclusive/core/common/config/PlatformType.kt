package com.flixclusive.core.common.config

enum class PlatformType {
    MOBILE,
    TV,
    ;

    val isMobile: Boolean get() = this == MOBILE
    val isTV: Boolean get() = this == TV
}
