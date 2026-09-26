package net.mamby.events

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.mamby.events.core.AppTextProvider
import net.mamby.events.core.AppThemePreference
import net.mamby.events.core.SettingsState
import net.mamby.events.core.SettingsStore
import net.mamby.events.features.settings.SettingsScreen
import net.mamby.events.features.settings.SettingsViewModel
import net.mamby.events.ui.LocalEventsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class SettingsScreenBehaviorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsScreen_renders_core_settings_sections_with_fakes() {
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsStore(),
            localizer = FakeTextProvider()
        )

        composeRule.setContent {
            LocalEventsTheme(darkTheme = false) {
                SettingsScreen(
                    onAppInfo = {},
                    onBack = {},
                    viewModel = viewModel
                )
            }
        }

        composeRule.onNodeWithText("SettingsTitle").assertIsDisplayed()
        composeRule.onAllNodesWithText("FavoritesTitle").assertCountEquals(0)
        composeRule.onAllNodesWithText("NoSavedEvents").assertCountEquals(0)
        composeRule.onNodeWithText("MediaLabel").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("AutoplayLabel").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Language").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Theme").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun languagePicker_selectsNonLatinLocale_andRestoresSystem() {
        val settingsStore = FakeSettingsStore()
        val viewModel = SettingsViewModel(
            settingsRepository = settingsStore,
            localizer = FakeTextProvider()
        )

        composeRule.setContent {
            LocalEventsTheme(darkTheme = false) {
                SettingsScreen(
                    onAppInfo = {},
                    onBack = {},
                    viewModel = viewModel
                )
            }
        }

        composeRule.onNodeWithText("Language").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(SystemLanguageLabel).performClick()
        composeRule.onNodeWithText("简体中文").assertIsDisplayed()
        composeRule.onNodeWithText("Search languages").performTextInput("日本")
        composeRule.onNodeWithText("日本語").assertIsDisplayed().performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            settingsStore.currentValue.selectedLanguageTag == "ja"
        }
        composeRule.onNodeWithText("日本語").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText(SystemLanguageLabel, useUnmergedTree = true)[0].performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            settingsStore.currentValue.selectedLanguageTag == null
        }
    }

    @Test
    fun languagePicker_keepsFirstControlBelowSheetChrome() {
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsStore(),
            localizer = FakeTextProvider()
        )
        composeRule.setContent {
            LocalEventsTheme(darkTheme = false) {
                SettingsScreen(onAppInfo = {}, onBack = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("Language").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(SystemLanguageLabel).performClick()

        val chromeBottom = composeRule.onNodeWithContentDescription("Close")
            .fetchSemanticsNode()
            .boundsInRoot
            .bottom
        val firstControlTop = composeRule.onNodeWithText("Search languages")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue(firstControlTop >= chromeBottom)
    }
}

private class FakeSettingsStore : SettingsStore {
    private val settingsState = MutableStateFlow(SettingsState(effectiveLanguage = "en"))
    val currentValue: SettingsState
        get() = settingsState.value

    override val settings: Flow<SettingsState> = settingsState

    override suspend fun current(): SettingsState = settingsState.value

    override suspend fun setLanguageTag(languageTag: String?) {
        settingsState.value = settingsState.value.copy(
            selectedLanguageTag = languageTag,
            effectiveLanguage = languageTag ?: "en"
        )
    }

    override suspend fun migrateLegacyLanguagePreference() = Unit

    override fun refreshLocaleState() = Unit

    override fun previewFloatingSurfaceOpacityLevel(level: Float) {}

    override suspend fun saveFloatingSurfaceOpacityLevel() {}

    override suspend fun setThemePreference(preference: AppThemePreference) {
        settingsState.value = settingsState.value.copy(themePreference = preference)
    }

    override suspend fun setMediaAutoplayEnabled(enabled: Boolean) {
        settingsState.value = settingsState.value.copy(mediaAutoplayEnabled = enabled)
    }

    override suspend fun setHouseholdChildAges(ages: List<Int>) {
        settingsState.value = settingsState.value.copy(householdChildAges = ages)
    }
}

private class FakeTextProvider : AppTextProvider {
    override fun get(key: String, language: String): String =
        when (key) {
            "SearchLanguagesPlaceholder" -> "Search a language"
            else -> key
        }

    override fun format(key: String, language: String, vararg args: Any?): String =
        when (key) {
            "LanguageSystemDefault" -> SystemLanguageLabel
            "ThemeSystemDefault" -> "System (${args.firstOrNull()?.toString().orEmpty()})"
            else -> args.foldIndexed(key) { index, current, value -> current.replace("{$index}", value?.toString().orEmpty()) }
        }
}

private const val SystemLanguageLabel = "System (English)"
