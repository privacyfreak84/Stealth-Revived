package com.cosmos.unreddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.model.preferences.BrandingPreferences
import com.cosmos.unreddit.data.repository.BrandingPreferencesRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UiViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    private val brandingPreferencesRepository: BrandingPreferencesRepository
) : ViewModel() {
    val policyDisclaimerShown: Flow<Boolean> =
        preferencesRepository.getPolicyDisclaimerShown(false).distinctUntilChanged()

    private val _navigationVisibility = MutableStateFlow(true)
    val navigationVisibility: StateFlow<Boolean> = _navigationVisibility

    fun setNavigationVisibility(visible: Boolean) {
        _navigationVisibility.updateValue(visible)
    }

    fun setColorScheme(scheme: BrandingPreferences.ColorScheme) {
        viewModelScope.launch { brandingPreferencesRepository.setColorScheme(scheme) }
    }
}
