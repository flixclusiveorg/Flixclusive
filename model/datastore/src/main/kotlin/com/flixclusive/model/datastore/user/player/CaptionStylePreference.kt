package com.flixclusive.model.datastore.user.player

import android.graphics.Typeface

enum class CaptionStylePreference(val typeface: Typeface) {
    Normal(Typeface.create("sans-serif", Typeface.NORMAL)),
    Bold(Typeface.create("sans-serif-medium", Typeface.BOLD)),
    Italic(Typeface.create("sans-serif-medium", Typeface.ITALIC)),
    Monospace(Typeface.create("monospace", Typeface.NORMAL));
}
