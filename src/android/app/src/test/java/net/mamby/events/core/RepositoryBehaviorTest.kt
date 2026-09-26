package net.mamby.events.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.mamby.events.testFeedItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryBehaviorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val stores = mutableListOf<DataStore<Preferences>>()
    private val scopes = mutableListOf<TestScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun favoritesRepository_adds_updates_and_removes_favorites() = runTest {
        val repository = FavoritesRepository(createDataStore("favorites"), json)
        val item = testFeedItem()

        repository.setFavorite(item, true)
        repository.setFavorite(item.copy(title = item.title.copy(default = "Updated")), true)
        repository.setFavorite(item, false)

        assertEquals(emptyList<EventFeedItem>(), repository.current())
    }

    @Test
    fun feedCacheRepository_rejects_empty_or_locationless_snapshots() = runTest {
        val repository = FeedCacheRepository(createDataStore("cache"), json)
        val invalidSnapshot = FeedSnapshot(
            language = "fr",
            query = FeedQuery(),
            page = 0,
            pageSize = 10,
            response = FeedPageResponse(items = emptyList(), generatedAt = "2026-05-23T00:00:00Z"),
            cachedAt = "2026-05-23T00:00:00Z"
        )

        repository.writeLastSnapshot(invalidSnapshot)

        assertNull(repository.readLastSnapshot())
    }

    @Test
    fun feedCacheRepository_round_trips_valid_snapshot() = runTest {
        val repository = FeedCacheRepository(createDataStore("cache-valid"), json)
        val location = FeedLocationScope(
            id = "Paris",
            name = "Paris",
            displayPath = "Paris, France",
            isSearchable = true
        )
        val snapshot = FeedSnapshot(
            language = "fr-FR",
            query = FeedQuery(locationScopeId = "PARIS", locationScope = location, text = " music "),
            page = 0,
            pageSize = 10,
            response = FeedPageResponse(
                items = listOf(testFeedItem()),
                generatedAt = "2026-05-23T00:00:00Z"
            ),
            cachedAt = "2026-05-23T00:00:00Z"
        )

        repository.writeLastSnapshot(snapshot)

        val cached = repository.readLastSnapshot()
        assertEquals("fr", cached?.language)
        assertEquals("paris", cached?.query?.attendanceScopeId)
        assertEquals("music", cached?.query?.text)
    }

    @Test
    fun searchHistoryRepository_deduplicates_and_sorts_recent_searches() = runTest {
        val repository = SearchHistoryRepository(createDataStore("history"), json)
        val location = FeedLocationScope(
            id = "paris",
            name = "Paris",
            displayPath = "Paris, France",
            isSearchable = true
        )
        val older = FeedSearchHistoryEntry(
            locationScopeId = "PARIS",
            locationScope = location,
            text = " music ",
            lastUsedAt = "2026-05-20T00:00:00Z"
        )
        val newer = older.copy(lastUsedAt = "2026-05-23T00:00:00Z")

        repository.save(older)
        repository.save(newer)

        val entries = repository.read()
        assertEquals(1, entries.size)
        assertEquals("music", entries.single().text)
        assertEquals("2026-05-23T00:00:00Z", entries.single().lastUsedAt)
    }

    @Test
    fun settingsRepository_persists_preferences() = runTest {
        val localeController = FakeAppLocaleController()
        val repository = SettingsRepository(createDataStore("settings"), json, localeController)

        repository.setLanguageTag("fr")
        repository.setThemePreference(AppThemePreference.Dark)
        repository.setMediaAutoplayEnabled(true)

        val settings = repository.settings.first()
        assertEquals("fr", settings.selectedLanguageTag)
        assertEquals(AppThemePreference.Dark, settings.themePreference)
        assertEquals(true, settings.mediaAutoplayEnabled)
        assertEquals("fr", settings.effectiveLanguage)
    }

    @Test
    fun settingsRepository_refreshLocaleState_emits_latest_locale_without_preference_change() = runTest {
        val localeController = FakeAppLocaleController()
        val repository = SettingsRepository(createDataStore("settings-locale-refresh"), json, localeController)
        val emissions = mutableListOf<SettingsState>()

        val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.settings.take(2).toList(emissions)
        }
        advanceUntilIdle()

        localeController.setSelectedLanguageTag("ja")
        repository.refreshLocaleState()
        advanceUntilIdle()

        assertEquals(listOf("en", "ja"), emissions.map { it.effectiveLanguage })
        collection.cancel()
    }

    @Test
    fun settingsRepository_defaults_to_manual_playback() = runTest {
        val repository = SettingsRepository(createDataStore("settings-defaults"), json, FakeAppLocaleController())

        val settings = repository.settings.first()

        assertEquals(false, settings.mediaAutoplayEnabled)
    }

    @Test
    fun settingsRepository_uses_legacy_autoplay_value_until_new_preference_is_written() = runTest {
        val dataStore = createDataStore("settings-legacy")
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("settings.audioAutoplayEnabled")] = true
        }
        val repository = SettingsRepository(dataStore, json, FakeAppLocaleController())

        assertEquals(true, repository.settings.first().mediaAutoplayEnabled)

        repository.setMediaAutoplayEnabled(false)

        assertEquals(false, repository.settings.first().mediaAutoplayEnabled)
    }

    private fun createDataStore(name: String): DataStore<Preferences> {
        val scope = TestScope(UnconfinedTestDispatcher() + Job())
        scopes += scope
        val file = File(temporaryFolder.root, "$name.preferences_pb")
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            .also(stores::add)
    }
}

private class FakeAppLocaleController : AppLocaleController {
    private var selectedLanguageTag: String? = null

    override fun selectedLanguageTag(): String? = selectedLanguageTag

    override fun systemLanguageTag(): String = "en"

    override fun effectiveLanguageTag(): String =
        selectedLanguageTag?.let(::normalizeAppLanguageTag) ?: "en"

    override fun setSelectedLanguageTag(languageTag: String?) {
        selectedLanguageTag = languageTag?.let(::normalizeAppLanguageTag)
    }
}
