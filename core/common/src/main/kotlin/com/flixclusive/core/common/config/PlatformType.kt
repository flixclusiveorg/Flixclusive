package com.flixclusive.core.common.config

enum class PlatformType {
    MOBILE,
    TV,
    ;

    override fun toString(): String {
        return when (this) {
            MOBILE -> "Mobile"
            TV -> "TV"
        }
    }
}
