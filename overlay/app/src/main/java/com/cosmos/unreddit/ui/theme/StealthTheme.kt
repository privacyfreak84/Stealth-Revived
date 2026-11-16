package com.cosmos.unreddit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cosmos.unreddit.data.model.preferences.BrandingPreferences

/** True when wallpaper-based colors can be used on this device. */
val supportsDynamicColor: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Resolves the Material 3 color scheme for the chosen palette, light or dark mode,
 * and the AMOLED night mode.
 */
@Composable
fun stealthColorScheme(
    scheme: BrandingPreferences.ColorScheme,
    darkTheme: Boolean,
    amoled: Boolean,
): ColorScheme {
    val context = LocalContext.current
    return remember(scheme, darkTheme, amoled, context) {
        val base = when {
            scheme == BrandingPreferences.ColorScheme.DYNAMIC && supportsDynamicColor ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> scheme.darkScheme()
            else -> scheme.lightScheme()
        }
        if (darkTheme && amoled) base.amoled() else base
    }
}

/**
 * Stealth Revived's Material 3 Expressive theme: expressive spring motion,
 * the expressive shape scale, and the emphasized type styles.
 */
@Composable
fun StealthTheme(
    scheme: BrandingPreferences.ColorScheme,
    amoled: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme() || amoled,
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = stealthColorScheme(scheme, darkTheme, amoled),
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes(),
        typography = Typography(),
        content = content,
    )
}
