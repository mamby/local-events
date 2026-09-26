package net.mamby.events.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settingsRepository: SettingsStore
) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState()
    )

    init {
        viewModelScope.launch {
            settingsRepository.migrateLegacyLanguagePreference()
        }
    }

    fun refreshLocaleState() {
        settingsRepository.refreshLocaleState()
    }
}
