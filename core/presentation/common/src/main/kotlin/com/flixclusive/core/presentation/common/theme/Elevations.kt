package com.flixclusive.core.presentation.common.theme

/**
 * Object to hold elevation values used throughout the app for consistency.
 * */
object Elevations {
    const val LEVEL_1 = 0.05f
    const val LEVEL_2 = 0.08f
    const val LEVEL_3 = 0.11f
    const val LEVEL_4 = 0.12f
    const val LEVEL_5 = 0.14f
    const val LEVEL_6 = 0.15f
    const val LEVEL_7 = 0.16f
    const val LEVEL_8 = 0.18f
    const val LEVEL_9 = 0.20f
    const val LEVEL_10 = 0.25f

    /**
     * Returns the elevation value for a given level.
     *
     * @param level The elevation level (1-10).
     * @return The corresponding elevation value as a Float.
     * */
    fun getElevationForLevel(level: Int): Float {
        return when (level) {
            1 -> LEVEL_1
            2 -> LEVEL_2
            3 -> LEVEL_3
            4 -> LEVEL_4
            5 -> LEVEL_5
            6 -> LEVEL_6
            7 -> LEVEL_7
            8 -> LEVEL_8
            9 -> LEVEL_9
            10 -> LEVEL_10
            else -> 0f
        }
    }
}
