package com.cosmos.unreddit.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cosmos.unreddit.data.model.preferences.BrandingPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrandingPreferencesRepository @Inject constructor(
    private val datastore: DataStore<Preferences>
) {
    fun getColorScheme(): Flow<BrandingPreferences.ColorScheme> =
        datastore.data.map { BrandingPreferences.ColorScheme.fromKey(it[Keys.COLOR_SCHEME]) }

    suspend fun setColorScheme(scheme: BrandingPreferences.ColorScheme) {
        datastore.edit { it[Keys.COLOR_SCHEME] = scheme.key }
    }

    private object Keys {
        val COLOR_SCHEME = stringPreferencesKey("brand_color_scheme")
    }
}
