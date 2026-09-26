package net.mamby.events.features.feed

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.mamby.events.MainDispatcherRule
import net.mamby.events.core.AppTextProvider
import net.mamby.events.core.AppThemePreference
import net.mamby.events.core.DateTextFormatter
import net.mamby.events.core.EventFeedItem
import net.mamby.events.core.FavoritesStore
import net.mamby.events.core.FeedCacheStore
import net.mamby.events.core.FeedCategoryOption
import net.mamby.events.core.FeedAttendanceScope
import net.mamby.events.core.FeedFilterKind
import net.mamby.events.core.FeedPageResponse
import net.mamby.events.core.FeedQuery
import net.mamby.events.core.FeedRemoteDataSource
import net.mamby.events.core.FeedSearchHistoryEntry
import net.mamby.events.core.FeedSearchParser
import net.mamby.events.core.FeedSnapshot
import net.mamby.events.core.SearchHistoryStore
import net.mamby.events.core.SettingsState
import net.mamby.events.core.SettingsStore
import net.mamby.events.core.TelemetryRecorder
import net.mamby.events.testFeedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelBehaviorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val location = FeedAttendanceScope(
        id = "paris",
        name = "Paris",
        displayPath = "Paris, France",
        isSearchable = true
    )

    @Test
    fun load_without_cache_starts_first_search_state() = runTest {
        val dependencies = FeedViewModelDependencies()
        val viewModel = dependencies.createViewModel()

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.contentState is FeedContentState.FirstSearch)
        assertTrue(state.hasCompletedInitialCacheRead)
        assertTrue(state.shouldAutoOpenFirstSearch)
        assertEquals(0, dependencies.feedApi.getFeedCalls.size)
    }

    @Test
    fun load_with_cached_snapshot_uses_cached_items() = runTest {
        val item = testFeedItem()
        val dependencies = FeedViewModelDependencies(
            cacheStore = FakeFeedCacheStore(
                snapshot = FeedSnapshot(
                    language = "en",
                    query = FeedQuery(attendanceScopeId = location.id, attendanceScope = location),
                    page = 0,
                    pageSize = 10,
                    response = FeedPageResponse(
                        items = listOf(item),
                        generatedAt = "2026-05-23T00:00:00Z",
                        refreshCursor = "cursor"
                    ),
                    cachedAt = "2026-05-23T00:00:00Z"
                )
            )
        )
        val viewModel = dependencies.createViewModel()

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.contentState is FeedContentState.Ready)
        assertEquals(listOf(item), state.items)
        assertEquals(0, state.currentIndex)
        assertFalse(state.shouldAutoOpenFirstSearch)
    }

    @Test
    fun applySearch_success_updates_items_and_history() = runTest {
        val item = testFeedItem()
        val dependencies = FeedViewModelDependencies()
        dependencies.feedApi.feedResponse = FeedPageResponse(
            items = listOf(item),
            generatedAt = "2026-05-23T00:00:00Z"
        )
        val viewModel = dependencies.createViewModel()

        viewModel.selectLocationSuggestion(location)
        viewModel.onSearchTextChanged("rooftop")
        val applied = viewModel.applySearch()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(applied)
        assertTrue(state.contentState is FeedContentState.Ready)
        assertEquals(listOf(item), state.items)
        assertEquals("paris", state.activeQuery.attendanceScopeId)
        assertEquals(1, dependencies.searchHistoryStore.entries.size)
        assertEquals("rooftop", dependencies.searchHistoryStore.entries.single().text)
    }

    @Test
    fun applySearch_failure_sets_search_error() = runTest {
        val dependencies = FeedViewModelDependencies()
        dependencies.feedApi.feedError = IllegalStateException("offline")
        val viewModel = dependencies.createViewModel()

        viewModel.selectLocationSuggestion(location)
        val applied = viewModel.applySearch()
        advanceUntilIdle()

        assertFalse(applied)
        assertEquals("SearchUnavailableError", viewModel.state.value.searchErrorText)
    }

    @Test
    fun selectCustomDateRange_replaces_date_filter_and_can_be_cleared() {
        val viewModel = FeedViewModelDependencies().createViewModel()

        viewModel.selectDateFilter(viewModel.dateFilterOptions().first())
        viewModel.selectCustomDateRange(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 9))

        val dateFilter = viewModel.state.value.appliedFilters.single { it.kind == FeedFilterKind.DateRange }
        assertEquals("2026-04-03", dateFilter.query.dateFrom)
        assertEquals("2026-04-09", dateFilter.query.dateTo)
        assertEquals("2026-04-03-2026-04-09", dateFilter.label)

        viewModel.selectDateFilter(null)

        assertTrue(viewModel.state.value.appliedFilters.none { it.kind == FeedFilterKind.DateRange })
    }

    @Test
    fun selectCustomDateRange_without_end_date_creates_single_day_filter() {
        val viewModel = FeedViewModelDependencies().createViewModel()

        viewModel.selectCustomDateRange(LocalDate.of(2026, 4, 3), null)

        val dateFilter = viewModel.state.value.appliedFilters.single { it.kind == FeedFilterKind.DateRange }
        assertEquals("2026-04-03", dateFilter.query.dateFrom)
        assertEquals("2026-04-03", dateFilter.query.dateTo)
    }

    @Test
    fun applySearch_sends_custom_date_range_to_feed_request() = runTest {
        val dependencies = FeedViewModelDependencies()
        dependencies.feedApi.feedResponse = FeedPageResponse(
            items = listOf(testFeedItem()),
            generatedAt = "2026-05-23T00:00:00Z"
        )
        val viewModel = dependencies.createViewModel()

        viewModel.selectLocationSuggestion(location)
        viewModel.selectCustomDateRange(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 9))
        assertTrue(viewModel.applySearch())
        advanceUntilIdle()

        val query = dependencies.feedApi.getFeedCalls.single()
        assertEquals("2026-04-03", query.dateFrom)
        assertEquals("2026-04-09", query.dateTo)
    }

    @Test
    fun toggleFavorite_updates_favorites_and_records_telemetry() = runTest {
        val dependencies = FeedViewModelDependencies()
        val viewModel = dependencies.createViewModel()
        val item = testFeedItem()

        viewModel.toggleFavorite(item)
        advanceUntilIdle()

        assertEquals(listOf(item.copy(isFavorited = true)), dependencies.favoritesStore.current())
        assertEquals(listOf(item.id to true), dependencies.telemetryRecorder.favoriteEvents)
    }
}

private data class FeedViewModelDependencies(
    val feedApi: FakeFeedRemoteDataSource = FakeFeedRemoteDataSource(),
    val cacheStore: FakeFeedCacheStore = FakeFeedCacheStore(),
    val searchHistoryStore: FakeSearchHistoryStore = FakeSearchHistoryStore(),
    val favoritesStore: FakeFavoritesStore = FakeFavoritesStore(),
    val telemetryRecorder: FakeTelemetryRecorder = FakeTelemetryRecorder(),
    val settingsStore: FakeSettingsStore = FakeSettingsStore(),
    val textProvider: FakeTextProvider = FakeTextProvider(),
    val dateFormatter: FakeDateFormatter = FakeDateFormatter()
) {
    fun createViewModel(): FeedViewModel =
        FeedViewModel(
            feedApi = feedApi,
            feedCacheRepository = cacheStore,
            searchHistoryRepository = searchHistoryStore,
            favoriteRepository = favoritesStore,
            telemetryRepository = telemetryRecorder,
            settingsRepository = settingsStore,
            searchParser = FeedSearchParser(textProvider),
            localizer = textProvider,
            dateFormatter = dateFormatter
        )
}

private class FakeFeedRemoteDataSource : FeedRemoteDataSource {
    var feedResponse = FeedPageResponse(items = emptyList(), generatedAt = "2026-05-23T00:00:00Z")
    var feedError: Throwable? = null
    val getFeedCalls = mutableListOf<FeedQuery>()
    var locationResults = emptyList<FeedAttendanceScope>()

    override suspend fun getFeed(
        language: String,
        query: FeedQuery,
        page: Int,
        pageSize: Int,
        knownCursor: String?
    ): FeedPageResponse {
        feedError?.let { throw it }
        getFeedCalls += query
        return feedResponse
    }

    override suspend fun searchAttendanceScopes(text: String, language: String, limit: Int): List<FeedAttendanceScope> =
        locationResults

    override suspend fun searchCategories(text: String, language: String, limit: Int): List<FeedCategoryOption> =
        emptyList()
}

private class FakeFeedCacheStore(
    var snapshot: FeedSnapshot? = null
) : FeedCacheStore {
    override suspend fun readLastSnapshot(): FeedSnapshot? = snapshot

    override suspend fun writeLastSnapshot(snapshot: FeedSnapshot) {
        this.snapshot = snapshot
    }
}

private class FakeSearchHistoryStore : SearchHistoryStore {
    val entries = mutableListOf<FeedSearchHistoryEntry>()

    override suspend fun read(): List<FeedSearchHistoryEntry> = entries

    override suspend fun save(entry: FeedSearchHistoryEntry) {
        entries.removeAll {
            it.attendanceScopeId.equals(entry.attendanceScopeId, ignoreCase = true) &&
                it.text?.trim().orEmpty().equals(entry.text?.trim().orEmpty(), ignoreCase = true)
        }
        entries += entry
    }
}

private class FakeFavoritesStore : FavoritesStore {
    private val favoritesState = MutableStateFlow<List<EventFeedItem>>(emptyList())

    override val favorites: Flow<List<EventFeedItem>> = favoritesState

    override suspend fun current(): List<EventFeedItem> = favoritesState.value

    override suspend fun setFavorite(item: EventFeedItem, isFavorited: Boolean) {
        favoritesState.value = if (isFavorited) {
            listOf(item.copy(isFavorited = true))
        } else {
            emptyList()
        }
    }

    override suspend fun refreshMetadata(items: List<EventFeedItem>) = Unit
}

private class FakeTelemetryRecorder : TelemetryRecorder {
    val favoriteEvents = mutableListOf<Pair<String, Boolean>>()

    override fun start() = Unit

    override fun stop() = Unit

    override suspend fun recordView(eventId: String) = Unit

    override suspend fun recordFavorite(eventId: String, isFavorited: Boolean) {
        favoriteEvents += eventId to isFavorited
    }

    override suspend fun recordShareIntent(eventId: String) = Unit
}

private class FakeSettingsStore : SettingsStore {
    private val settingsState = MutableStateFlow(SettingsState(effectiveLanguage = "en"))

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
    override fun get(key: String, language: String): String = key

    override fun format(key: String, language: String, vararg args: Any?): String =
        args.foldIndexed(key) { index, current, value -> current.replace("{$index}", value?.toString().orEmpty()) }
}

private class FakeDateFormatter : DateTextFormatter {
    override fun formatEventStart(startDate: String, language: String): String = startDate

    override fun formatFeedDate(startDate: String, language: String): String = startDate

    override fun formatOccurrence(startsAt: String, endsAt: String?, language: String): String = startsAt

    override fun formatDateRange(from: String?, to: String?, language: String): String =
        listOfNotNull(from, to).joinToString("-")
}
