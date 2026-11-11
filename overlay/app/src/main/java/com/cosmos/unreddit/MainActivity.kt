package com.cosmos.unreddit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.cosmos.unreddit.ui.main.StealthApp
import com.cosmos.unreddit.ui.theme.StealthTheme
import com.cosmos.unreddit.util.extension.unredditApplication
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /** Bumped when the palette or AMOLED mode changes, so the theme is read again. */
    private var themeRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(unredditApplication.appTheme)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            @Suppress("UNUSED_EXPRESSION") themeRevision
            StealthTheme(
                scheme = unredditApplication.colorScheme,
                amoled = unredditApplication.isAmoled,
            ) {
                StealthApp(onColorSchemeChanged = { themeRevision++ })
            }
        }
    }
}
