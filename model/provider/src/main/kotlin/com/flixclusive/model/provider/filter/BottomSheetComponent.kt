package com.flixclusive.model.provider.filter

/**
 * A sealed class representing the UI components that could be displayed on the filter bottom sheet.
 *
 * This class originally came from Tachiyomi.
 *
 * @param name The name of the filter.
 * @param state The state of the filter.
 */
@Suppress("unused")
sealed class BottomSheetComponent<T>(
    name: String,
    state: T
) : Filter<T>(name, state) {
    /**
     *
     * A UI divider (horizontal). Useful for visual distinction between sections.
     *
     * ![Divider image](https://developer.android.com/images/reference/androidx/compose/material3/divider.png)
     *
     * */
    data object HorizontalDivider
        : BottomSheetComponent<Nothing?>("", null)

    /**
     *
     * A simple text header. Useful for separating sections in the list or showing any note or warning to the user.
     *
     * @param label The text to display.
     * */
    open class HeaderLabel(label: String)
        : BottomSheetComponent<Nothing?>(label, null)

    /**
     *
     * Spacer also acts as a padding/margin. Useful for visual distinction between sections.
     *
     * @param dp The number of dp to add as padding. **REMINDER: DP != Pixels**
     * */
    class Spacer(dp: Int)
        : BottomSheetComponent<Int>("", dp)
}
