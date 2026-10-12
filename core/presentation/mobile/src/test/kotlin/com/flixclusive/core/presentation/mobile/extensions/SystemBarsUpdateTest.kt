package com.flixclusive.core.presentation.mobile.extensions

import androidx.core.view.WindowInsetsControllerCompat
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class SystemBarsUpdateTest {
    @Test
    fun `show while the bars are not visible only shows and reapplies insets`() {
        val hidden = systemBarsUpdate(isVisible = true, systemBarsVisible = false)
        val unknownInsets = systemBarsUpdate(isVisible = true, systemBarsVisible = null)

        expectThat(hidden.behavior).isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        expectThat(hidden.hideBeforeShow).isFalse()
        expectThat(hidden.reapplyInsets).isTrue()

        expectThat(unknownInsets.behavior).isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        expectThat(unknownInsets.hideBeforeShow).isFalse()
        expectThat(unknownInsets.reapplyInsets).isTrue()
    }

    @Test
    fun `show while the bars are already visible hides them before showing`() {
        val update = systemBarsUpdate(isVisible = true, systemBarsVisible = true)

        expectThat(update.behavior).isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        expectThat(update.hideBeforeShow).isTrue()
        expectThat(update.reapplyInsets).isTrue()
    }

    @Test
    fun `hide keeps transient swipe behavior and does not reapply insets`() {
        val update = systemBarsUpdate(isVisible = false, systemBarsVisible = true)

        expectThat(update.behavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        expectThat(update.hideBeforeShow).isFalse()
        expectThat(update.reapplyInsets).isFalse()
    }
}
