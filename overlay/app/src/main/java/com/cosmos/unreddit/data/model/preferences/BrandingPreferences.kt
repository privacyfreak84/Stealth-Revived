package com.cosmos.unreddit.data.model.preferences

object BrandingPreferences {
    enum class ColorScheme(val key: String) {
        TOKYO_NIGHT("tokyo-night"), NORD("nord"), GRUVBOX("gruvbox"),
        DRACULA("dracula"), CATPPUCCIN("catppuccin"),

        /** Wallpaper-based Material You colors; falls back to Tokyo Night before Android 12. */
        DYNAMIC("dynamic");

        companion object {
            fun fromKey(key: String?): ColorScheme =
                values().firstOrNull { it.key == key } ?: TOKYO_NIGHT
        }
    }
}
