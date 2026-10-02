package net.mamby.events.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mamby.events.core.AppTextProvider
import net.mamby.events.core.AppThemePreference
import net.mamby.events.core.SettingsStore
import net.mamby.events.core.SettingsState
import net.mamby.events.core.SupportedAppLanguages
import net.mamby.androidkit.compose.form.AndroidKitSettingsStore

data class SettingsUiState(
    val settings: SettingsState = SettingsState()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val kitSettingsStore: AndroidKitSettingsStore,
    private val settingsRepository: SettingsStore,
    private val localizer: AppTextProvider
) : ViewModel() {
    val state = settingsRepository.settings.map { settings ->
        SettingsUiState(settings = settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun string(key: String, language: String = state.value.settings.effectiveLanguage): String =
        localizer.get(key, language)

    fun formatString(
        key: String,
        language: String = state.value.settings.effectiveLanguage,
        vararg args: Any?
    ): String =
        localizer.format(key, language, *args)

    fun languageOptions(
        language: String = state.value.settings.effectiveLanguage,
        systemLanguageTag: String = state.value.settings.systemLanguageTag
    ): List<String> =
        listOf(
            formatString("LanguageSystemDefault", language, nativeLanguageLabel(systemLanguageTag))
        ) + SupportedAppLanguages.map { nativeLanguageLabel(it.tag) }

    fun themeOptions(
        language: String = state.value.settings.effectiveLanguage,
        systemDarkTheme: Boolean
    ): List<String> =
        listOf(
            formatString("ThemeSystemDefault", language, systemThemeLabel(language, systemDarkTheme)),
            string("ThemeLight", language),
            string("ThemeDark", language)
        )

    fun setMediaAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMediaAutoplayEnabled(enabled)
        }
    }

    fun setLanguageIndex(index: Int) {
        viewModelScope.launch {
            settingsRepository.setLanguageTag(SupportedAppLanguages.getOrNull(index - 1)?.tag)
        }
    }

    fun previewFloatingSurfaceOpacityLevel(level: Float) =
        settingsRepository.previewFloatingSurfaceOpacityLevel(level)

    fun saveFloatingSurfaceOpacityLevel() {
        viewModelScope.launch { settingsRepository.saveFloatingSurfaceOpacityLevel() }
    }

    fun setThemeIndex(index: Int) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(
                when (index) {
                    1 -> AppThemePreference.Light
                    2 -> AppThemePreference.Dark
                    else -> AppThemePreference.System
                }
            )
        }
    }

    private fun systemThemeLabel(language: String, systemDarkTheme: Boolean): String =
        string(if (systemDarkTheme) "ThemeDark" else "ThemeLight", language)

    internal fun nativeLanguageLabel(languageTag: String): String {
        if (languageTag == "zh-Hans") {
            return "简体中文"
        }

        val locale = Locale.forLanguageTag(languageTag)
        val displayLanguage = locale.getDisplayLanguage(locale).ifBlank { languageTag }
        return displayLanguage.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase(locale) else first.toString()
        }
    }
}
