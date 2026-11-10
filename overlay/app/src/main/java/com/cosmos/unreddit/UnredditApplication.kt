package com.cosmos.unreddit

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.cosmos.unreddit.data.model.preferences.BrandingPreferences
import com.cosmos.unreddit.data.model.preferences.UiPreferences
import com.cosmos.unreddit.data.repository.BrandingPreferencesRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.util.FileUncaughtExceptionHandler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class UnredditApplication : Application(), ImageLoaderFactory, Configuration.Provider {
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var brandingPreferencesRepository: BrandingPreferencesRepository
    @Inject lateinit var workerFactory: HiltWorkerFactory

    var colorScheme: BrandingPreferences.ColorScheme = BrandingPreferences.ColorScheme.TOKYO_NIGHT
        private set
    private var forceAmoled = false

    var appTheme: Int
        get() = if (forceAmoled) R.style.AmoledAppTheme else when (colorScheme) {
            BrandingPreferences.ColorScheme.TOKYO_NIGHT -> R.style.AppTheme_TokyoNight
            BrandingPreferences.ColorScheme.NORD -> R.style.AppTheme_Nord
            BrandingPreferences.ColorScheme.GRUVBOX -> R.style.AppTheme_Gruvbox
            BrandingPreferences.ColorScheme.DRACULA -> R.style.AppTheme_Dracula
            BrandingPreferences.ColorScheme.CATPPUCCIN -> R.style.AppTheme_Catppuccin
        }
        set(mode) {
            forceAmoled = UiPreferences.NightMode.isAmoled(mode)
            AppCompatDelegate.setDefaultNightMode(
                if (forceAmoled) AppCompatDelegate.MODE_NIGHT_YES else mode
            )
        }

    fun applyColorScheme(scheme: BrandingPreferences.ColorScheme) {
        colorScheme = scheme
    }

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            colorScheme = brandingPreferencesRepository.getColorScheme().first()
            appTheme = preferencesRepository.getNightMode(AppCompatDelegate.MODE_NIGHT_YES).first()
        }
        Thread.setDefaultUncaughtExceptionHandler(FileUncaughtExceptionHandler(this))
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(applicationContext).components {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(ImageDecoderDecoder.Factory())
            else add(GifDecoder.Factory())
        }.crossfade(true).build()

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder().setWorkerFactory(workerFactory).build()
}
