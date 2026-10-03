package net.mamby.events.features.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import android.widget.Toast
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mamby.androidkit.compose.form.AndroidKitFloatingOpacitySetting
import net.mamby.androidkit.compose.form.AndroidKitLanguageSetting
import net.mamby.androidkit.compose.form.AndroidKitSettingsAbout
import net.mamby.androidkit.compose.form.AndroidKitSettingsLink
import net.mamby.androidkit.compose.form.AndroidKitSettingsOption
import net.mamby.androidkit.compose.form.AndroidKitSettingsPage
import net.mamby.androidkit.compose.form.AndroidKitSettings
import net.mamby.androidkit.compose.form.AndroidKitSettingsSearchPage
import net.mamby.androidkit.compose.form.AndroidKitSettingsSelection
import net.mamby.androidkit.compose.form.AndroidKitSettingsSystemOption
import net.mamby.androidkit.compose.form.androidKitSettingsCatalog
import net.mamby.events.BuildConfig
import net.mamby.events.R
import net.mamby.events.core.AppThemePreference
import net.mamby.events.core.SupportedAppLanguages

@Composable
fun SettingsScope(
    onAppInfo: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val failureMessage = stringResource(R.string.settings_save_failed)
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val language = ui.settings.effectiveLanguage
    val languageLabels = viewModel.languageOptions(language, ui.settings.systemLanguageTag)
    val languageIds = listOf("system") + SupportedAppLanguages.map { it.tag }
    val systemDarkTheme = isSystemInDarkTheme()
    val themeLabels = viewModel.themeOptions(language, systemDarkTheme)
    val themeIds = listOf(AppThemePreference.System, AppThemePreference.Light, AppThemePreference.Dark)
    val mediaIcon = ImageVector.vectorResource(R.drawable.icon_settings_media)
    val appearanceTitle = stringResource(R.string.settings_appearance)
    val about = appInfo()
    val catalog = androidKitSettingsCatalog {
        main(key = MainSettingsPageKey, title = viewModel.string("SettingsTitle", language)) {
            section(key = "media", label = viewModel.string("MediaLabel", language)) {
                toggle(
                    key = "autoplay",
                    label = viewModel.string("AutoplayLabel", language),
                    persistence = viewModel.kitSettingsStore.setting(booleanPreferencesKey("settings.mediaAutoplayEnabled"), false),
                    onCheckedChange = viewModel::setMediaAutoplayEnabled,
                    icon = mediaIcon,
                )
            }
            section(key = "language") {
                language(AndroidKitLanguageSetting(
                    selection = AndroidKitSettingsSelection(
                        persistence = viewModel.kitSettingsStore.setting(stringPreferencesKey("selected_language_tag"), ui.settings.selectedLanguageTag ?: "system"),
                        options = languageIds.drop(1).zip(languageLabels.drop(1)) { id, label -> AndroidKitSettingsOption(id, label) },
                        onSelected = { viewModel.setLanguageIndex(languageIds.indexOf(it)) },
                        systemOption = AndroidKitSettingsSystemOption(
                            id = "system",
                            currentValueLabel = viewModel.nativeLanguageLabel(ui.settings.systemLanguageTag),
                        ),
                    ),
                ))
            }
            section(key = "appearance", label = appearanceTitle) {
                theme(AndroidKitSettingsSelection(
                    persistence = viewModel.kitSettingsStore.setting(stringPreferencesKey("settings.theme"), AppThemePreference.System.name),
                    options = themeIds.drop(1).zip(themeLabels.drop(1)) { id, label -> AndroidKitSettingsOption(id.name, label) },
                    onSelected = { id -> viewModel.setThemeIndex(themeIds.indexOfFirst { it.name == id }) },
                    systemOption = AndroidKitSettingsSystemOption(
                        id = AppThemePreference.System.name,
                        currentValueLabel = themeLabels[if (systemDarkTheme) 2 else 1],
                    ),
                ))
                transparency(AndroidKitFloatingOpacitySetting(
                    persistence = viewModel.kitSettingsStore.setting(floatPreferencesKey("settings.floatingSurfaceOpacityLevel"), net.mamby.androidkit.compose.theme.AndroidKitFloatingSurfaceDefaults.DefaultOpacityLevel),
                    onValueChange = viewModel::previewFloatingSurfaceOpacityLevel,
                    onValueChangeFinished = { viewModel.saveFloatingSurfaceOpacityLevel() },
                ))
            }
        }
        about(key = AboutSettingsPageKey, content = about, onOpen = onAppInfo)
    }
    AndroidKitSettings(catalog, viewModel.kitSettingsStore, onOpenSearch,
        onStorageFailure = { Toast.makeText(context, failureMessage, Toast.LENGTH_LONG).show() },
        content = content)
}

@Composable
fun SettingsScreen(onBack: () -> Unit, searchVisible: Boolean, onCloseSearch: () -> Unit) {
    SettingsDestination(MainSettingsPageKey, onBack, searchVisible, onCloseSearch)
}

@Composable
fun AppInfoScreen(onBack: () -> Unit, searchVisible: Boolean, onCloseSearch: () -> Unit) {
    SettingsDestination(AboutSettingsPageKey, onBack, searchVisible, onCloseSearch)
}

@Composable
private fun SettingsDestination(pageKey: String, onBack: () -> Unit, searchVisible: Boolean, onCloseSearch: () -> Unit) {
    BackHandler(enabled = searchVisible, onBack = onCloseSearch)
    if (searchVisible) {
        AndroidKitSettingsSearchPage(onBack = onCloseSearch)
    } else {
        AndroidKitSettingsPage(pageKey = pageKey, onBack = onBack)
    }
}

@Composable
private fun appInfo(): AndroidKitSettingsAbout {
    val context = LocalContext.current
    fun link(url: String) = AndroidKitSettingsLink(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
    )
    return AndroidKitSettingsAbout(
        appName = stringResource(R.string.app_name),
        version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        privacyPolicy = link("https://example.com/privacy"),
        termsOfUse = link("https://example.com/terms"),
        libraries = link("$RepositoryUrl/blob/main/THIRD-PARTY-NOTICES.md"),
        sourceCode = link(RepositoryUrl),
        website = link("https://example.com"),
        contact = link("https://example.com/contact"),
    )
}

private const val MainSettingsPageKey = "main"
private const val AboutSettingsPageKey = "about"
private const val RepositoryUrl = "https://github.com/mamby/local-events"
