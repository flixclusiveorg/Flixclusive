package com.flixclusive.feature.mobile.library.manage.extension

internal fun <T> Collection<T>.containsAny(other: Collection<T>): Boolean {
    return other.any { it in this }
}
