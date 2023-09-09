package com.flixclusive_provider.utils

import java.io.Reader

object OkHttpUtils {
    fun Reader?.asString(): String? {
        return use {
            val string = it?.readText()
            it?.close()

            return@use string
        }
    }
}