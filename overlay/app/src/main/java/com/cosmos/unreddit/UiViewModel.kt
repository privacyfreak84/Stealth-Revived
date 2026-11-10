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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UiViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    private val brandingPreferencesRepository: BrandingPreferencesRepository
) : ViewModel() {
    val leftHandedMode: Flow<Boolean> = preferencesRepository.getLeftHandedMode().distinctUntilChanged()
    val policyDisclaimerShown: Flow<Boolean> = preferencesRepository.getPolicyDisclaimerShown(false).distinctUntilChanged()
    val colorScheme: StateFlow<BrandingPreferences.ColorScheme> =
        brandingPreferencesRepository.getColorScheme().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), BrandingPreferences.ColorScheme.TOKYO_NIGHT
        )
    val navigationLayout: StateFlow<BrandingPreferences.NavigationLayout> =
        brandingPreferencesRepository.getNavigationLayout().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), BrandingPreferences.NavigationLayout.SIDE_MENU
        )
    private val _navigationVisibility = MutableStateFlow(true)
    val navigationVisibility: StateFlow<Boolean> = _navigationVisibility
    fun setNavigationVisibility(visible: Boolean) { _navigationVisibility.updateValue(visible) }
    fun setColorScheme(scheme: BrandingPreferences.ColorScheme) {
        viewModelScope.launch { brandingPreferencesRepository.setColorScheme(scheme) }
    }
    fun setNavigationLayout(layout: BrandingPreferences.NavigationLayout) {
        viewModelScope.launch { brandingPreferencesRepository.setNavigationLayout(layout) }
    }
}
