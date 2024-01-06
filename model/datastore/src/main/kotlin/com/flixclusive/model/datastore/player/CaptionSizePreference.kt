package com.flixclusive.model.datastore.player

enum class CaptionSizePreference {
    Small,
    Medium,
    Large;

    companion object {
        private const val SMALL_FONT_SIZE = 18F // Equivalent to 20dp
        private const val MEDIUM_FONT_SIZE = 20F // Equivalent to 20dp
        private const val LARGE_FONT_SIZE = 24F

        fun CaptionSizePreference.getDp(isTv: Boolean = false): Float {
            return when(this) {
                Large -> LARGE_FONT_SIZE + (if(isTv) 4F else 0F)
                Medium -> MEDIUM_FONT_SIZE + (if(isTv) 4F else 0F)
                Small -> SMALL_FONT_SIZE + (if(isTv) 2F else 0F)
            }
        }
    }
}