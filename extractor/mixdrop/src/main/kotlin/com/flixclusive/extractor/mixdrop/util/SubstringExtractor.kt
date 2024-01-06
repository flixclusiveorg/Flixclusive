package com.flixclusive.extractor.mixdrop.util


/**
 *
 * Source (Aniyomi):
 * https://github.com/aniyomiorg/aniyomi-extensions/blob/master/lib/unpacker/src/main/java/eu/kanade/tachiyomi/lib/unpacker/SubstringExtractor.kt
 *
 *
 */
class SubstringExtractor(private val text: String) {
    private var startIndex = 0

    fun skipOver(str: String) {
        val index = text.indexOf(str, startIndex)
        if (index == -1) return
        startIndex = index + str.length
    }

    fun substringBefore(str: String): String {
        val index = text.indexOf(str, startIndex)
        if (index == -1) return ""
        val result = text.substring(startIndex, index)
        startIndex = index + str.length
        return result
    }

    fun substringBetween(left: String, right: String): String {
        val index = text.indexOf(left, startIndex)
        if (index == -1) return ""
        val leftIndex = index + left.length
        val rightIndex = text.indexOf(right, leftIndex)
        if (rightIndex == -1) return ""
        startIndex = rightIndex + right.length
        return text.substring(leftIndex, rightIndex)
    }
}