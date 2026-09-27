package net.mamby.events.features.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import net.mamby.androidkit.compose.form.AndroidKitSettingsSearchConfiguration
import net.mamby.androidkit.compose.form.AndroidKitSettingsSearchPage
import net.mamby.androidkit.compose.form.AndroidKitSettingsSelection
import net.mamby.androidkit.compose.form.AndroidKitSettingsSystemOption
import net.mamby.androidkit.compose.form.androidKitSettingsCatalog
import net.mamby.events.BuildConfig
import net.mamby.events.R
import net.mamby.events.core.AppThemePreference
import net.mamby.events.core.SupportedAppLanguages

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAppInfo: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var recentQueries by rememberSaveable { mutableStateOf(emptyList<String>()) }
    BackHandler(enabled = searchVisible) {
        searchVisible = false
    }
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val language = ui.settings.effectiveLanguage
    val languageLabels = viewModel.languageOptions(language, ui.settings.systemLanguageTag)
    val languageIds = listOf("system") + SupportedAppLanguages.map { it.tag }
    val systemDarkTheme = isSystemInDarkTheme()
    val themeLabels = viewModel.themeOptions(language, systemDarkTheme)
    val themeIds = listOf(AppThemePreference.System, AppThemePreference.Light, AppThemePreference.Dark)
    val mediaIcon = ImageVector.vectorResource(R.drawable.icon_settings_media)
    val generalTitle = stringResource(R.string.settings_general)
    val about = appInfo()
    val catalog = androidKitSettingsCatalog(search = AndroidKitSettingsSearchConfiguration(
        onOpenSearch = { searchVisible = true },
        recentQueries = recentQueries,
        onRecentQueriesChange = { recentQueries = it },
    )) {
        main(key = MainSettingsPageKey, title = viewModel.string("SettingsTitle", language)) {
            section(key = "media", label = viewModel.string("MediaLabel", language)) {
                toggle(
                    key = "autoplay",
                    label = viewModel.string("AutoplayLabel", language),
                    checked = ui.settings.mediaAutoplayEnabled,
                    onCheckedChange = viewModel::setMediaAutoplayEnabled,
                    icon = mediaIcon,
                )
            }
            section(key = "general", label = generalTitle) {
                language(AndroidKitLanguageSetting(
                    selection = AndroidKitSettingsSelection(
                        options = languageIds.drop(1).zip(languageLabels.drop(1)) { id, label -> AndroidKitSettingsOption(id, label) },
                        selectedId = ui.settings.selectedLanguageTag?.takeIf { it in languageIds } ?: "system",
                        onSelected = { viewModel.setLanguageIndex(languageIds.indexOf(it)) },
                        systemOption = AndroidKitSettingsSystemOption(
                            id = "system",
                            currentValueLabel = viewModel.nativeLanguageLabel(ui.settings.systemLanguageTag),
                        ),
                    ),
                ))
                theme(AndroidKitSettingsSelection(
                    options = themeIds.drop(1).zip(themeLabels.drop(1)) { id, label -> AndroidKitSettingsOption(id.name, label) },
                    selectedId = ui.settings.themePreference.name,
                    onSelected = { id -> viewModel.setThemeIndex(themeIds.indexOfFirst { it.name == id }) },
                    systemOption = AndroidKitSettingsSystemOption(
                        id = AppThemePreference.System.name,
                        currentValueLabel = themeLabels[if (systemDarkTheme) 2 else 1],
                    ),
                ))
                transparency(AndroidKitFloatingOpacitySetting(
                    value = ui.settings.floatingSurfaceOpacityLevel,
                    onValueChange = viewModel::previewFloatingSurfaceOpacityLevel,
                    onValueChangeFinished = { viewModel.saveFloatingSurfaceOpacityLevel() },
                ))
            }
        }
        about(key = AboutSettingsPageKey, content = about, onOpen = onAppInfo)
    }
    if (searchVisible) {
        AndroidKitSettingsSearchPage(catalog = catalog, onBack = { searchVisible = false })
    } else {
        AndroidKitSettingsPage(catalog = catalog, pageKey = MainSettingsPageKey, onBack = onBack)
    }
}

@Composable
fun AppInfoScreen(onBack: () -> Unit) {
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var recentQueries by rememberSaveable { mutableStateOf(emptyList<String>()) }
    BackHandler(enabled = searchVisible) {
        searchVisible = false
    }
    val title = stringResource(R.string.settings_title)
    val about = appInfo()
    val catalog = androidKitSettingsCatalog(search = AndroidKitSettingsSearchConfiguration(
        onOpenSearch = { searchVisible = true },
        recentQueries = recentQueries,
        onRecentQueriesChange = { recentQueries = it },
    )) {
        main(key = MainSettingsPageKey, title = title)
        about(key = AboutSettingsPageKey, content = about, onOpen = {})
    }
    if (searchVisible) {
        AndroidKitSettingsSearchPage(catalog = catalog, onBack = { searchVisible = false })
    } else {
        AndroidKitSettingsPage(catalog = catalog, pageKey = AboutSettingsPageKey, onBack = onBack)
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
