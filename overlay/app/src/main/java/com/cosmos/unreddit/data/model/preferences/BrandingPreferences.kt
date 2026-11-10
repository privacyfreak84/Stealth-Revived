package com.cosmos.unreddit.data.model.preferences

object BrandingPreferences {
    enum class ColorScheme(val key: String) {
        TOKYO_NIGHT("tokyo-night"), NORD("nord"), GRUVBOX("gruvbox"),
        DRACULA("dracula"), CATPPUCCIN("catppuccin");
        companion object {
            fun fromKey(key: String?): ColorScheme =
                values().firstOrNull { it.key == key } ?: TOKYO_NIGHT
        }
    }
    enum class NavigationLayout(val key: String) {
        SIDE_MENU("side-menu"), CLASSIC("classic");
        companion object {
            fun fromKey(key: String?): NavigationLayout =
                values().firstOrNull { it.key == key } ?: SIDE_MENU
        }
    }
}
