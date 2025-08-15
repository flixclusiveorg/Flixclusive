package com.flixclusive.feature.mobile.library.manage

import com.flixclusive.core.strings.UiText
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.core.strings.R as LocaleR

data object ItemCount : LibrarySortFilter {
    override val displayName = UiText.from(LocaleR.string.item_count)
}
