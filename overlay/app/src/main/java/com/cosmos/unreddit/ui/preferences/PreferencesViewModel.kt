package com.cosmos.unreddit.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.model.preferences.DataPreferences
import com.cosmos.unreddit.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val nightMode: SharedFlow<Int> = preferencesRepository.getNightMode()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    val leftHandedMode: Flow<Boolean> = preferencesRepository.getLeftHandedMode()
    val showNsfw: Flow<Boolean> = preferencesRepository.getShowNsfw()
    val showNsfwPreview: Flow<Boolean> = preferencesRepository.getShowNsfwPreview()
    val showSpoilerPreview: Flow<Boolean> = preferencesRepository.getShowSpoilerPreview()

    val redditSource: SharedFlow<Pair<Int, String>> = preferencesRepository.getRedditSource()
        .map { DataPreferences.RedditSource.REDDIT.value to "" }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val privacyEnhancerEnabled: SharedFlow<Boolean> = preferencesRepository
        .getPrivacyEnhancerEnabled()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    // Kept so the existing preferences screen interface remains compatible.
    val tedditInstances: List<String> = emptyList()

    fun setNightMode(nightMode: Int) = viewModelScope.launch {
        preferencesRepository.setNightMode(nightMode)
    }

    fun setLeftHandedMode(leftHandedMode: Boolean) = viewModelScope.launch {
        preferencesRepository.setLeftHandedMode(leftHandedMode)
    }

    fun setShowNsfw(showNsfw: Boolean) = viewModelScope.launch {
        preferencesRepository.setShowNsfw(showNsfw)
    }

    fun setShowNsfwPreview(showNsfwPreview: Boolean) = viewModelScope.launch {
        preferencesRepository.setShowNsfwPreview(showNsfwPreview)
    }

    fun setShowSpoilerPreview(showSpoilerPreview: Boolean) = viewModelScope.launch {
        preferencesRepository.setShowSpoilerPreview(showSpoilerPreview)
    }

    @Suppress("UNUSED_PARAMETER")
    fun setRedditSource(source: Int, instance: String?) {
        viewModelScope.launch {
            preferencesRepository.setRedditSource(DataPreferences.RedditSource.REDDIT.value)
            preferencesRepository.setRedditSourceInstance("")
        }
    }
}
