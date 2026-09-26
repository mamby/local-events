package net.mamby.events.features.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.mamby.events.core.AppTextProvider
import net.mamby.events.core.AttendanceSummaryText
import net.mamby.events.core.DateTextFormatter
import net.mamby.events.core.EventFeedItem
import net.mamby.events.core.FavoritesStore
import net.mamby.events.core.FeedAttendanceScope
import net.mamby.events.core.FeedCacheStore
import net.mamby.events.core.FeedCategoryOption
import net.mamby.events.core.FeedFilterChip
import net.mamby.events.core.FeedFilterKind
import net.mamby.events.core.FeedPageResponse
import net.mamby.events.core.FeedPriceFilter
import net.mamby.events.core.FeedQuery
import net.mamby.events.core.FeedSearchHistoryEntry
import net.mamby.events.core.FeedSearchParser
import net.mamby.events.core.FeedSnapshot
import net.mamby.events.core.FeedTermOption
import net.mamby.events.core.FeedRemoteDataSource
import net.mamby.events.core.PriceSummaryText
import net.mamby.events.core.SearchHistoryStore
import net.mamby.events.core.SearchDraftStore
import net.mamby.events.core.SettingsStore
import net.mamby.events.core.TelemetryRecorder
import net.mamby.events.core.attendanceDetailLines
import net.mamby.events.core.attendanceSummaryText
import net.mamby.events.core.accessibleForAgeDisplayText
import net.mamby.events.core.accessibilityFilterDisplayText
import net.mamby.events.core.categoryDisplayText
import net.mamby.events.core.localized
import net.mamby.events.core.normalizeAppLanguageTag
import net.mamby.events.core.occurrenceDetailLines
import net.mamby.events.core.priceDetailLines
import net.mamby.events.core.priceDisplayText
import net.mamby.events.core.priceSummaryText
import net.mamby.events.core.toAttendanceScope

sealed interface FeedContentState {
    data object Loading : FeedContentState
    data object FirstSearch : FeedContentState
    data object Ready : FeedContentState
    data object Empty : FeedContentState
    data class Error(val message: String) : FeedContentState
}

data class FeedSearchHistoryItem(
    val entry: FeedSearchHistoryEntry,
    val title: String,
    val subtitle: String
)

enum class FeedSearchCriterionIcon {
    Location,
    Filter
}

data class FeedSearchCriterion(
    val text: String,
    val icon: FeedSearchCriterionIcon? = null
)

enum class FeedSearchSheetPage {
    Search,
    Terms,
    Locations,
    Filters,
    Recent
}

data class FeedUiState(
    val language: String = "en",
    val contentLanguage: String = "en",
    val items: List<EventFeedItem> = emptyList(),
    val contentState: FeedContentState = FeedContentState.Loading,
    val hasCompletedInitialCacheRead: Boolean = false,
    val currentIndex: Int = -1,
    val page: Int = 0,
    val isTopOverlayVisible: Boolean = false,
    val isLoadingMore: Boolean = false,
    val activeQuery: FeedQuery = FeedQuery(),
    val pendingNewResultsCount: Int = 0,
    val searchTerms: List<String> = emptyList(),
    val selectedLocationScopes: List<FeedAttendanceScope> = emptyList(),
    val customLocations: List<String> = emptyList(),
    val termSearchText: String = "",
    val termSuggestions: List<FeedTermOption> = emptyList(),
    val isTermSearchLoading: Boolean = false,
    val termSearchErrorText: String = "",
    val searchText: String = "",
    val parsedSearchText: String? = null,
    val locationSearchText: String = "",
    val selectedLocationScope: FeedAttendanceScope? = null,
    val locationSuggestions: List<FeedAttendanceScope> = emptyList(),
    val isLocationSearchLoading: Boolean = false,
    val locationSearchErrorText: String = "",
    val suggestedFilters: List<FeedFilterChip> = emptyList(),
    val appliedFilters: List<FeedFilterChip> = emptyList(),
    val searchSheetPage: FeedSearchSheetPage = FeedSearchSheetPage.Search,
    val categorySearchText: String = "",
    val accessibilitySearchText: String = "",
    val accessibleAgeText: String = "",
    val householdChildAges: List<Int> = emptyList(),
    val categoryOptions: List<FeedCategoryOption> = emptyList(),
    val isCategorySearchLoading: Boolean = false,
    val categorySearchErrorText: String = "",
    val searchHistory: List<FeedSearchHistoryItem> = emptyList(),
    val searchErrorKey: String? = null,
    val searchErrorText: String = "",
    val showLocationRequiredError: Boolean = false,
    val isSearchApplying: Boolean = false,
    val shouldAutoOpenFirstSearch: Boolean = false
) {
    val hasLocationSuggestions: Boolean = locationSuggestions.isNotEmpty()
    val hasTermSuggestions: Boolean = termSuggestions.isNotEmpty()
    val hasSuggestedFilters: Boolean = suggestedFilters.isNotEmpty()
    val hasAppliedFilters: Boolean = appliedFilters.isNotEmpty()
    val hasSearchError: Boolean = searchErrorText.isNotBlank()
    val hasCategorySearchError: Boolean = categorySearchErrorText.isNotBlank()
    val hasAccessibleAgeError: Boolean =
        accessibleAgeText.isNotBlank() && accessibleAgeText.toIntOrNull()?.takeIf { it in 0..120 } == null
    val optionalFilterCount: Int = appliedFilters.size
    val hasPendingNewResults: Boolean = pendingNewResultsCount > 0
    val hasLocationInputError: Boolean = false
    val hasCommittedSearchCriteria: Boolean =
        searchTerms.isNotEmpty() ||
            selectedLocationScopes.isNotEmpty() ||
            customLocations.isNotEmpty() ||
            hasAppliedFilters
    val canSubmitSearch: Boolean = hasCommittedSearchCriteria && !isSearchApplying
    val canShowSearchHistory: Boolean =
        searchHistory.isNotEmpty() &&
            !hasCommittedSearchCriteria
    val hasSearchDraft: Boolean = hasCommittedSearchCriteria
    val canAddCustomTerm: Boolean =
        termSearchText.trim().length >= 2 &&
            !isTermSearchLoading &&
            termSuggestions.none { it.value.equals(termSearchText.trim(), ignoreCase = true) } &&
            searchTerms.none { it.equals(termSearchText.trim(), ignoreCase = true) }
    val canAddCustomLocation: Boolean =
        locationSearchText.trim().length >= 2 &&
            !isLocationSearchLoading &&
            locationSuggestions.none {
                it.name.equals(locationSearchText.trim(), ignoreCase = true) ||
                    it.displayPath.equals(locationSearchText.trim(), ignoreCase = true)
            } &&
            selectedLocationScopes.none {
                it.name.equals(locationSearchText.trim(), ignoreCase = true) ||
                    it.displayPath.equals(locationSearchText.trim(), ignoreCase = true)
            } &&
            customLocations.none { it.equals(locationSearchText.trim(), ignoreCase = true) }
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedApi: FeedRemoteDataSource,
    private val feedCacheRepository: FeedCacheStore,
    private val searchHistoryRepository: SearchHistoryStore,
    private val searchDraftRepository: SearchDraftStore = EmptySearchDraftStore,
    private val favoriteRepository: FavoritesStore,
    private val telemetryRepository: TelemetryRecorder,
    private val settingsRepository: SettingsStore,
    private val searchParser: FeedSearchParser,
    private val localizer: AppTextProvider,
    private val dateFormatter: DateTextFormatter
) : ViewModel() {
    private val _state = MutableStateFlow(FeedUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<FeedEvent>()
    val events: SharedFlow<FeedEvent> = _events.asSharedFlow()

    private var hasLoadedInitialFeed = false
    private var isLoadingFeed = false
    private var isFavoriteSyncStarted = false
    private var locationSearchJob: Job? = null
    private var termSearchJob: Job? = null
    private var categorySearchJob: Job? = null
    private var draftPersistenceJob: Job? = null
    private var pendingRefreshSnapshot: FeedSnapshot? = null

    fun load() {
        if (hasLoadedInitialFeed || isLoadingFeed) {
            return
        }

        viewModelScope.launch {
            isLoadingFeed = true
            _state.update { it.copy(contentState = FeedContentState.Loading) }

            try {
                val draft = runCatching { searchDraftRepository.read()?.query }.getOrNull()
                val snapshot = feedCacheRepository.readLastSnapshot()
                if (snapshot != null) {
                    hasLoadedInitialFeed = true
                    applyCachedSnapshot(snapshot)
                    applySearchDraft(draft)
                    isLoadingFeed = false
                    startPostInitialLoadWork(snapshot.response.refreshCursor)
                    return@launch
                }

                val settings = settingsRepository.current()
                val language = settings.effectiveLanguage
                hasLoadedInitialFeed = true
                _state.update {
                    it.copy(
                        language = language,
                        contentLanguage = language,
                        items = emptyList(),
                        currentIndex = -1,
                        page = 0,
                        activeQuery = FeedQuery(),
                        householdChildAges = settings.householdChildAges,
                        contentState = FeedContentState.FirstSearch,
                        hasCompletedInitialCacheRead = true,
                        isTopOverlayVisible = true,
                        shouldAutoOpenFirstSearch = true
                    )
                }
                applySearchDraft(draft)
                isLoadingFeed = false
                startPostInitialLoadWork(knownCursor = null)
            } catch (_: Throwable) {
                hasLoadedInitialFeed = true
                _state.update {
                    it.copy(
                        items = emptyList(),
                        currentIndex = -1,
                        contentState = FeedContentState.Error(localizer.get("FeedLoadErrorMessage", it.language)),
                        hasCompletedInitialCacheRead = true,
                        isTopOverlayVisible = true,
                        shouldAutoOpenFirstSearch = false
                    )
                }
            } finally {
                isLoadingFeed = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (isLoadingFeed || _state.value.contentState == FeedContentState.FirstSearch) {
                return@launch
            }

            isLoadingFeed = true
            val current = _state.value
            _state.update { it.copy(contentState = FeedContentState.Loading, pendingNewResultsCount = 0) }

            try {
                val settings = settingsRepository.current()
                val requestedLanguage = settings.effectiveLanguage
                val response = feedApi.getFeed(requestedLanguage, current.activeQuery, 0, PageSize, null)
                val snapshot = createSnapshot(requestedLanguage, current.activeQuery, 0, response)
                if (_state.value.language != normalizeAppLanguageTag(requestedLanguage)) {
                    return@launch
                }
                feedCacheRepository.writeLastSnapshot(snapshot)
                applySnapshot(snapshot)
            } catch (_: Throwable) {
                _state.update {
                    it.copy(contentState = if (it.items.isEmpty()) FeedContentState.Empty else FeedContentState.Ready)
                }
            } finally {
                isLoadingFeed = false
            }
        }
    }

    fun updateCurrentIndex(index: Int) {
        val current = _state.value
        if (index < 0 || index >= current.items.size) {
            return
        }

        _state.update { it.copy(currentIndex = index) }

        if (current.items.size - index <= 2) {
            loadMore()
        }
    }

    fun onLanguageChanged(language: String) {
        val normalizedLanguage = normalizeAppLanguageTag(language)
        if (!hasLoadedInitialFeed) {
            return
        }

        if (_state.value.language == normalizedLanguage) {
            return
        }

        viewModelScope.launch {
            syncLanguageAfterInitialLoad(normalizedLanguage)
        }
    }

    fun openTermEditor() {
        _state.update {
            it.copy(
                searchSheetPage = FeedSearchSheetPage.Terms,
                termSearchText = "",
                termSuggestions = emptyList(),
                isTermSearchLoading = false,
                termSearchErrorText = ""
            )
        }
    }

    fun onSearchTextChanged(value: String) {
        updateDraft { current ->
            current.copy(searchTerms = value.trim().takeIf(String::isNotBlank)?.let(::listOf).orEmpty())
        }
    }

    fun onTermSearchTextChanged(value: String) {
        termSearchJob?.cancel()
        _state.update {
            it.copy(
                termSearchText = value,
                termSuggestions = emptyList(),
                isTermSearchLoading = value.trim().length >= 2,
                termSearchErrorText = ""
            )
        }

        val searchText = value.trim()
        if (searchText.length < 2) {
            return
        }

        termSearchJob = viewModelScope.launch {
            delay(250)
            try {
                val suggestions = feedApi.searchTerms(searchText, _state.value.language)
                    .filterNot { suggestion ->
                        _state.value.searchTerms.any { it.equals(suggestion.value, ignoreCase = true) }
                    }
                if (_state.value.termSearchText.trim().equals(searchText, ignoreCase = true)) {
                    _state.update {
                        it.copy(
                            termSuggestions = suggestions,
                            isTermSearchLoading = false,
                            termSearchErrorText = ""
                        )
                    }
                }
            } catch (_: Throwable) {
                if (_state.value.termSearchText.trim().equals(searchText, ignoreCase = true)) {
                    _state.update {
                        it.copy(
                            termSuggestions = emptyList(),
                            isTermSearchLoading = false,
                            termSearchErrorText = localizer.get("SearchSuggestionsUnavailable", it.language)
                        )
                    }
                }
            }
        }
    }

    fun selectTermSuggestion(option: FeedTermOption) {
        addSearchTerm(option.value)
    }

    fun addCustomTerm() {
        addSearchTerm(_state.value.termSearchText)
    }

    fun removeSearchTerm(term: String) {
        updateDraft { current ->
            current.copy(searchTerms = current.searchTerms.filterNot { it.equals(term, ignoreCase = true) })
        }
    }

    fun openLocationEditor() {
        _state.update {
            it.copy(
                searchSheetPage = FeedSearchSheetPage.Locations,
                locationSearchText = "",
                locationSuggestions = emptyList(),
                isLocationSearchLoading = false,
                locationSearchErrorText = ""
            )
        }
    }

    fun onLocationSearchTextChanged(value: String) {
        locationSearchJob?.cancel()
        _state.update {
            it.copy(
                locationSearchText = value,
                locationSuggestions = emptyList(),
                isLocationSearchLoading = value.trim().length >= 2,
                locationSearchErrorText = ""
            )
        }

        val searchText = value.trim()
        if (searchText.length < 2) {
            return
        }

        locationSearchJob = viewModelScope.launch {
            delay(250)
            try {
                val suggestions = feedApi.searchAttendanceScopes(searchText, _state.value.language)
                    .filterNot { suggestion ->
                        _state.value.selectedLocationScopes.any { it.id.equals(suggestion.id, ignoreCase = true) }
                    }
                if (_state.value.locationSearchText.trim().equals(searchText, ignoreCase = true)) {
                    _state.update {
                        it.copy(
                            locationSuggestions = suggestions,
                            isLocationSearchLoading = false,
                            locationSearchErrorText = ""
                        )
                    }
                }
            } catch (_: Throwable) {
                if (_state.value.locationSearchText.trim().equals(searchText, ignoreCase = true)) {
                    _state.update {
                        it.copy(
                            locationSuggestions = emptyList(),
                            isLocationSearchLoading = false,
                            locationSearchErrorText = localizer.get("SearchSuggestionsUnavailable", it.language)
                        )
                    }
                }
            }
        }
    }

    fun selectLocationSuggestion(locationScope: FeedAttendanceScope) {
        if (!locationScope.isSearchable || locationScope.id.isBlank()) {
            return
        }

        locationSearchJob?.cancel()
        updateDraft { current ->
            current.copy(
                selectedLocationScopes = (
                    current.selectedLocationScopes + locationScope.normalize()
                    ).distinctBy { it.id.lowercase() },
                locationSearchText = "",
                locationSuggestions = emptyList(),
                isLocationSearchLoading = false,
                locationSearchErrorText = ""
            )
        }
    }

    fun addCustomLocation() {
        val location = _state.value.locationSearchText.trim()
        if (location.length < 2) {
            return
        }
        updateDraft { current ->
            current.copy(
                customLocations = (current.customLocations + location)
                    .distinctBy(String::lowercase),
                locationSearchText = "",
                locationSuggestions = emptyList(),
                isLocationSearchLoading = false,
                locationSearchErrorText = ""
            )
        }
    }

    fun removeLocationScope(locationScope: FeedAttendanceScope) {
        updateDraft { current ->
            current.copy(
                selectedLocationScopes = current.selectedLocationScopes
                    .filterNot { it.id.equals(locationScope.id, ignoreCase = true) }
            )
        }
    }

    fun removeCustomLocation(location: String) {
        updateDraft { current ->
            current.copy(customLocations = current.customLocations.filterNot { it.equals(location, ignoreCase = true) })
        }
    }

    fun openFilterEditor() {
        _state.update {
            it.copy(
                searchSheetPage = FeedSearchSheetPage.Filters,
                categorySearchText = "",
                accessibilitySearchText = it.appliedFilters
                    .firstOrNull { filter -> filter.kind == FeedFilterKind.Accessibility }
                    ?.query
                    ?.accessibility
                    .orEmpty(),
                accessibleAgeText = it.appliedFilters
                    .firstOrNull { filter -> filter.kind == FeedFilterKind.AgeRestriction }
                    ?.query
                    ?.accessibleForAge
                    ?.toString()
                    .orEmpty(),
                categoryOptions = emptyList(),
                isCategorySearchLoading = false,
                categorySearchErrorText = ""
            )
        }
        viewModelScope.launch {
            val ages = runCatching { settingsRepository.current().householdChildAges }.getOrDefault(emptyList())
            _state.update { it.copy(householdChildAges = ages) }
        }
        onCategorySearchTextChanged("")
    }

    fun returnToSearchSummary() {
        termSearchJob?.cancel()
        locationSearchJob?.cancel()
        categorySearchJob?.cancel()
        _state.update {
            it.copy(
                searchSheetPage = FeedSearchSheetPage.Search,
                termSearchText = "",
                termSuggestions = emptyList(),
                isTermSearchLoading = false,
                termSearchErrorText = "",
                locationSearchText = "",
                locationSuggestions = emptyList(),
                isLocationSearchLoading = false,
                locationSearchErrorText = "",
                categorySearchText = "",
                accessibilitySearchText = "",
                accessibleAgeText = "",
                categoryOptions = emptyList(),
                isCategorySearchLoading = false,
                categorySearchErrorText = ""
            )
        }
    }

    fun prepareSearchSheet() {
        returnToSearchSummary()
        _state.update { it.copy(searchErrorKey = null, searchErrorText = "") }
    }

    fun onSearchSheetDismissed() {
        returnToSearchSummary()
        persistDraft()
    }

    fun onCategorySearchTextChanged(value: String) {
        categorySearchJob?.cancel()
        val searchText = value.trim()
        _state.update {
            it.copy(
                categorySearchText = value,
                categoryOptions = emptyList(),
                isCategorySearchLoading = true,
                categorySearchErrorText = ""
            )
        }

        categorySearchJob = viewModelScope.launch {
            if (searchText.isNotEmpty()) {
                delay(250)
            }

            try {
                val results = feedApi.searchCategories(searchText, _state.value.language)
                if (_state.value.searchSheetPage == FeedSearchSheetPage.Filters &&
                    _state.value.categorySearchText.trim().equals(searchText, ignoreCase = true)
                ) {
                    _state.update {
                        it.copy(
                            categoryOptions = results,
                            isCategorySearchLoading = false,
                            categorySearchErrorText = ""
                        )
                    }
                }
            } catch (_: Throwable) {
                if (_state.value.searchSheetPage == FeedSearchSheetPage.Filters &&
                    _state.value.categorySearchText.trim().equals(searchText, ignoreCase = true)
                ) {
                    _state.update {
                        it.copy(
                            categoryOptions = emptyList(),
                            isCategorySearchLoading = false,
                            categorySearchErrorText = localizer.get("CategorySearchUnavailableError", it.language)
                        )
                    }
                }
            }
        }
    }

    fun acceptSuggestedFilter(filter: FeedFilterChip) {
        setOptionalFilter(filter)
    }

    fun selectCategoryOption(option: FeedCategoryOption?) {
        if (option == null) {
            clearOptionalFilter(FeedFilterKind.Category)
            return
        }

        setOptionalFilter(
            FeedFilterChip(
                kind = FeedFilterKind.Category,
                label = option.label,
                query = FeedQuery(category = option.value)
            )
        )
    }

    fun selectDateFilter(filter: FeedFilterChip?) {
        if (filter == null) {
            clearOptionalFilter(FeedFilterKind.DateRange)
            return
        }

        setOptionalFilter(filter)
    }

    fun selectCustomDateRange(startDate: LocalDate, endDate: LocalDate?) {
        val inclusiveEndDate = endDate ?: startDate
        setOptionalFilter(
            FeedFilterChip(
                kind = FeedFilterKind.DateRange,
                label = dateFormatter.formatDateRange(startDate.toString(), inclusiveEndDate.toString(), _state.value.language),
                query = FeedQuery(dateFrom = startDate.toString(), dateTo = inclusiveEndDate.toString())
            )
        )
    }

    fun selectPriceFilter(priceFilter: FeedPriceFilter) {
        if (priceFilter == FeedPriceFilter.Any) {
            clearOptionalFilter(FeedFilterKind.Price)
            return
        }

        setOptionalFilter(
            FeedFilterChip(
                kind = FeedFilterKind.Price,
                label = priceDisplayText(priceFilter, _state.value.language, localizer),
                query = FeedQuery(priceFilter = priceFilter)
            )
        )
    }

    fun onAccessibilitySearchTextChanged(value: String) {
        val accessibility = value.trim()
        updateDraft { current ->
            val remainingFilters = current.appliedFilters.filterNot { it.kind == FeedFilterKind.Accessibility }
            val filters = if (accessibility.isEmpty()) {
                remainingFilters
            } else {
                remainingFilters + FeedFilterChip(
                    kind = FeedFilterKind.Accessibility,
                    label = accessibilityFilterDisplayText(accessibility, current.language, localizer),
                    query = FeedQuery(accessibility = accessibility)
                )
            }

            current.copy(accessibilitySearchText = value, appliedFilters = filters)
        }
    }

    fun selectHouseholdAgeFilter() {
        _state.value.householdChildAges.minOrNull()?.let(::applyAccessibleAgeFilter)
    }

    fun onAccessibleAgeTextChanged(value: String) {
        val age = value.trim().toIntOrNull()?.takeIf { it in 0..120 }
        updateDraft { current ->
            val remainingFilters = current.appliedFilters.filterNot { it.kind == FeedFilterKind.AgeRestriction }
            current.copy(
                accessibleAgeText = value,
                appliedFilters = if (age == null) {
                    remainingFilters
                } else {
                    remainingFilters + ageFilter(age, current.language)
                }
            )
        }
    }

    fun clearAccessibleAgeFilter() {
        updateDraft {
            it.copy(
                accessibleAgeText = "",
                appliedFilters = it.appliedFilters.filterNot { filter -> filter.kind == FeedFilterKind.AgeRestriction }
            )
        }
    }

    fun dateFilterOptions(): List<FeedFilterChip> {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val saturday = when (today.dayOfWeek.value) {
            6 -> today
            7 -> today.minusDays(1)
            else -> today.plusDays((6 - today.dayOfWeek.value).toLong())
        }

        return listOf(
            FeedFilterChip(
                kind = FeedFilterKind.DateRange,
                label = string("FilterToday"),
                query = FeedQuery(dateFrom = today.toString(), dateTo = today.toString())
            ),
            FeedFilterChip(
                kind = FeedFilterKind.DateRange,
                label = string("FilterTomorrow"),
                query = FeedQuery(dateFrom = tomorrow.toString(), dateTo = tomorrow.toString())
            ),
            FeedFilterChip(
                kind = FeedFilterKind.DateRange,
                label = string("FilterThisWeekend"),
                query = FeedQuery(dateFrom = saturday.toString(), dateTo = saturday.plusDays(1).toString())
            )
        )
    }

    fun clearOptionalFilters() {
        updateDraft { current ->
            current.copy(
                appliedFilters = emptyList(),
                suggestedFilters = emptyList(),
                accessibilitySearchText = "",
                accessibleAgeText = ""
            )
        }
    }

    fun removeAppliedFilter(filter: FeedFilterChip) {
        updateDraft { current ->
            current.copy(
                appliedFilters = current.appliedFilters.filterNot { it.sameFilter(filter) },
                accessibilitySearchText = if (filter.kind == FeedFilterKind.Accessibility) {
                    ""
                } else {
                    current.accessibilitySearchText
                },
                accessibleAgeText = if (filter.kind == FeedFilterKind.AgeRestriction) "" else current.accessibleAgeText
            )
        }
    }

    fun restoreSearchHistory(historyItem: FeedSearchHistoryItem) {
        val query = historyItem.entry.query?.normalize() ?: return
        applySearchDraft(query)
        persistDraft()
    }

    fun removeSearchHistory(historyItem: FeedSearchHistoryItem) {
        viewModelScope.launch {
            searchHistoryRepository.remove(historyItem.entry)
            refreshSearchHistory(_state.value.language)
        }
    }

    fun openSearchHistory() {
        if (_state.value.searchHistory.isNotEmpty()) {
            _state.update { it.copy(searchSheetPage = FeedSearchSheetPage.Recent) }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clear()
            refreshSearchHistory(_state.value.language)
        }
    }

    fun clearSearchDraft() {
        val pendingPersistence = draftPersistenceJob
        pendingPersistence?.cancel()
        applySearchDraft(null)
        viewModelScope.launch {
            pendingPersistence?.join()
            searchDraftRepository.clear()
        }
    }

    suspend fun applySearch(): Boolean {
        val current = _state.value
        if (current.isSearchApplying) {
            return false
        }
        if (!current.hasCommittedSearchCriteria) {
            return false
        }

        val query = buildSearchQuery()
        flushDraft(query)
        _state.update {
            it.copy(
                isSearchApplying = true,
                searchErrorKey = null,
                searchErrorText = "",
                showLocationRequiredError = false
            )
        }

        return try {
            val response = feedApi.getFeed(current.language, query, 0, PageSize, null)
            val snapshot = createSnapshot(current.language, query, 0, response)
            feedCacheRepository.writeLastSnapshot(snapshot)
            saveSearchHistory(query)
            query.accessibleForAge?.let { age ->
                settingsRepository.setHouseholdChildAges(listOf(age))
                _state.update { it.copy(householdChildAges = listOf(age)) }
            }
            searchDraftRepository.clear()
            pendingRefreshSnapshot = null
            applySnapshot(snapshot)
            applySearchDraft(null)
            _events.emit(FeedEvent.SearchApplied)
            true
        } catch (_: Throwable) {
            _state.update {
                it.copy(
                    searchErrorKey = "SearchUnavailableError",
                    searchErrorText = localizer.get("SearchUnavailableError", it.language)
                )
            }
            false
        } finally {
            _state.update { it.copy(isSearchApplying = false) }
        }
    }

    fun applyPendingRefresh() {
        val snapshot = pendingRefreshSnapshot ?: return
        pendingRefreshSnapshot = null
        viewModelScope.launch {
            applySnapshot(snapshot)
            _state.update { it.copy(pendingNewResultsCount = 0) }
        }
    }

    fun toggleFavorite(item: EventFeedItem) {
        viewModelScope.launch {
            val isFavorited = !item.isFavorited
            favoriteRepository.setFavorite(item, isFavorited)
            telemetryRepository.recordFavorite(item.id, isFavorited)
        }
    }

    fun recordView(item: EventFeedItem) {
        viewModelScope.launch {
            telemetryRepository.recordView(item.id)
        }
    }

    fun recordShareIntent(item: EventFeedItem) {
        viewModelScope.launch {
            telemetryRepository.recordShareIntent(item.id)
        }
    }

    fun markAutoOpenConsumed() {
        _state.update { it.copy(shouldAutoOpenFirstSearch = false) }
    }

    fun string(key: String): String =
        localizer.get(key, _state.value.language)

    fun formatString(key: String, vararg args: Any?): String =
        localizer.format(key, _state.value.language, *args)

    private fun contentString(key: String): String =
        localizer.get(key, _state.value.contentLanguage)

    private fun contentFormatString(key: String, vararg args: Any?): String =
        localizer.format(key, _state.value.contentLanguage, *args)

    fun categoryText(item: EventFeedItem): String =
        item.category.resolve(_state.value.contentLanguage)

    fun titleText(item: EventFeedItem): String =
        item.title.resolve(_state.value.contentLanguage)

    fun descriptionText(item: EventFeedItem): String =
        item.description.resolve(_state.value.contentLanguage)

    fun longDescriptionText(item: EventFeedItem): String =
        item.longDescription?.resolve(_state.value.contentLanguage) ?: descriptionText(item)

    fun accessibilityText(item: EventFeedItem): String =
        item.accessibility?.resolve(_state.value.contentLanguage).orEmpty()

    fun ageRestrictionText(item: EventFeedItem): String =
        when (val minimumAge = item.ageRestriction?.minimumRequiredAge) {
            null -> if (item.ageRestriction == null) {
                contentString("AgeRestrictionUnknown")
            } else {
                contentString("AgeRestrictionPublic")
            }
            else -> contentFormatString("AgeRestrictionMinimumFormat", minimumAge)
        }

    fun recommendedAgeText(item: EventFeedItem): String =
        item.recommendedAge?.let { recommended ->
            recommended.maximumAge?.let { maximumAge ->
                contentFormatString("RecommendedAgeRangeFormat", recommended.minimumAge, maximumAge)
            } ?: contentFormatString("RecommendedAgeMinimumFormat", recommended.minimumAge)
        }.orEmpty()

    fun feedDateText(item: EventFeedItem): String =
        dateFormatter.formatFeedDate(item.startDate, _state.value.contentLanguage)

    fun priceSummary(item: EventFeedItem): PriceSummaryText =
        priceSummaryText(item, _state.value.contentLanguage, localizer)

    fun attendanceSummary(item: EventFeedItem): AttendanceSummaryText =
        attendanceSummaryText(
            item = item,
            selectedScopeIds = _state.value.activeQuery.attendanceScopeIds,
            customLocations = _state.value.activeQuery.locations,
            language = _state.value.contentLanguage,
            localizer = localizer
        )

    fun attendanceDetails(item: EventFeedItem): List<String> =
        attendanceDetailLines(item, _state.value.contentLanguage, localizer)

    fun occurrenceDetails(item: EventFeedItem): List<String> =
        occurrenceDetailLines(item, _state.value.contentLanguage, dateFormatter)

    fun priceDetails(item: EventFeedItem): List<String> =
        priceDetailLines(item, _state.value.contentLanguage, localizer)

    fun fullDetailsText(item: EventFeedItem): String =
        listOf(
            categoryText(item),
            titleText(item),
            descriptionText(item),
            occurrenceDetails(item).joinToString(separator = "\n"),
            priceDetails(item).joinToString(separator = "\n"),
            attendanceDetails(item).joinToString(separator = "\n"),
            ageRestrictionText(item),
            recommendedAgeText(item)
        ).filter(String::isNotBlank).joinToString(separator = "\n")

    fun selectedLocationName(): String =
        (_state.value.selectedLocationScopes.map(FeedAttendanceScope::displayPath) + _state.value.customLocations)
            .joinToString()
            .ifBlank { string("ChooseAttendance") }

    fun pendingNewResultsText(): String =
        when (val count = _state.value.pendingNewResultsCount) {
            in 1..Int.MAX_VALUE -> formatString("NewResultsFormat", count)
            else -> ""
        }

    fun searchPillCriteria(): List<FeedSearchCriterion> {
        val query = _state.value.activeQuery.normalize()
        if (query.isEmpty) {
            return listOf(FeedSearchCriterion(string("FindEvents")))
        }

        val criteria = buildList {
            query.attendanceScopes
                .filter(FeedAttendanceScope::isSearchable)
                .map(FeedAttendanceScope::displayPath)
                .filter(String::isNotBlank)
                .forEach { add(FeedSearchCriterion(it, FeedSearchCriterionIcon.Location)) }

            query.locations.forEach {
                add(FeedSearchCriterion(it, FeedSearchCriterionIcon.Location))
            }
            query.terms.forEach { add(FeedSearchCriterion(it)) }
            query.category?.let {
                add(
                    FeedSearchCriterion(
                        categoryDisplayText(it, _state.value.language, localizer),
                        FeedSearchCriterionIcon.Filter
                    )
                )
            }

            query.accessibility?.let {
                add(
                    FeedSearchCriterion(
                        accessibilityFilterDisplayText(it, _state.value.language, localizer),
                        FeedSearchCriterionIcon.Filter
                    )
                )
            }

            query.accessibleForAge?.let {
                add(
                    FeedSearchCriterion(
                        accessibleForAgeDisplayText(it, _state.value.language, localizer),
                        FeedSearchCriterionIcon.Filter
                    )
                )
            }

            if (!query.dateFrom.isNullOrBlank() || !query.dateTo.isNullOrBlank()) {
                add(
                    FeedSearchCriterion(
                        dateFormatter.formatDateRange(query.dateFrom, query.dateTo, _state.value.language),
                        FeedSearchCriterionIcon.Filter
                    )
                )
            }

            if (query.priceFilter != FeedPriceFilter.Any) {
                add(
                    FeedSearchCriterion(
                        priceDisplayText(query.priceFilter, _state.value.language, localizer),
                        FeedSearchCriterionIcon.Filter
                    )
                )
            }
        }

        return criteria.ifEmpty { listOf(FeedSearchCriterion(string("FindEvents"))) }
    }

    private fun loadMore() {
        if (isLoadingFeed || _state.value.isLoadingMore || _state.value.items.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                val current = _state.value
                val nextPage = current.page + 1
                val response = feedApi.getFeed(current.language, current.activeQuery, nextPage, PageSize, null)
                favoriteRepository.refreshMetadata(response.items)
                val favorites = favoriteRepository.current().map { it.id }.toSet()
                _state.update {
                    it.copy(
                        items = it.items + response.items.map { item -> item.copy(isFavorited = item.id in favorites) },
                        page = nextPage,
                        contentState = FeedContentState.Ready
                    )
                }
            } catch (_: Throwable) {
                _state.update { it.copy(contentState = if (it.items.isEmpty()) FeedContentState.Empty else FeedContentState.Ready) }
            } finally {
                _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private fun queueBackgroundRefresh(knownCursor: String?) {
        viewModelScope.launch {
            try {
                val current = _state.value
                val response = feedApi.getFeed(current.language, current.activeQuery, 0, PageSize, knownCursor)
                favoriteRepository.refreshMetadata(response.items)
                val snapshot = createSnapshot(current.language, current.activeQuery, 0, response)
                feedCacheRepository.writeLastSnapshot(snapshot)

                if (response.newSinceCount > 0 &&
                    _state.value.language == current.language &&
                    _state.value.activeQuery.normalize() == current.activeQuery.normalize()
                ) {
                    pendingRefreshSnapshot = snapshot
                    _state.update { it.copy(pendingNewResultsCount = response.newSinceCount) }
                }
            } catch (_: Throwable) {
                // Background refresh should not disturb cached content.
            }
        }
    }

    private fun applyCachedSnapshot(snapshot: FeedSnapshot) {
        val query = snapshot.query.normalize()
        _state.update {
            it.copy(
                language = snapshot.language,
                contentLanguage = snapshot.language,
                items = snapshot.response.items,
                currentIndex = 0,
                page = snapshot.page,
                activeQuery = query,
                contentState = FeedContentState.Ready,
                hasCompletedInitialCacheRead = true,
                isTopOverlayVisible = true,
                pendingNewResultsCount = 0,
                shouldAutoOpenFirstSearch = false,
                searchTerms = emptyList(),
                selectedLocationScopes = emptyList(),
                customLocations = emptyList(),
                termSearchText = "",
                termSuggestions = emptyList(),
                searchText = "",
                parsedSearchText = null,
                selectedLocationScope = null,
                locationSearchText = "",
                locationSuggestions = emptyList(),
                appliedFilters = emptyList(),
                suggestedFilters = emptyList(),
                searchSheetPage = FeedSearchSheetPage.Search,
                categorySearchText = "",
                accessibilitySearchText = "",
                categoryOptions = emptyList(),
                isCategorySearchLoading = false,
                categorySearchErrorText = "",
                showLocationRequiredError = false,
                searchErrorKey = null,
                searchErrorText = ""
            )
        }
    }

    private fun startPostInitialLoadWork(knownCursor: String?) {
        startFavoriteSync()

        viewModelScope.launch {
            val settings = runCatching { settingsRepository.current() }.getOrNull()
            val language = settings?.effectiveLanguage ?: _state.value.language
            _state.update {
                it.copy(householdChildAges = settings?.householdChildAges ?: it.householdChildAges)
            }
            val normalizedLanguage = normalizeAppLanguageTag(language)
            val languageChanged = _state.value.language != normalizedLanguage

            if (languageChanged) {
                syncLanguageAfterInitialLoad(normalizedLanguage)
            } else {
                updateSearchStateForLanguage(normalizedLanguage)
                refreshSearchHistory(normalizedLanguage)
            }

            if (!languageChanged && _state.value.contentState != FeedContentState.FirstSearch) {
                queueBackgroundRefresh(knownCursor)
            }
        }
    }

    private fun startFavoriteSync() {
        if (isFavoriteSyncStarted) {
            return
        }

        isFavoriteSyncStarted = true
        viewModelScope.launch {
            favoriteRepository.favorites.collect { favorites ->
                val favoriteIds = favorites.map { it.id }.toSet()
                _state.update { current ->
                    current.copy(items = current.items.map { it.copy(isFavorited = it.id in favoriteIds) })
                }
            }
        }
    }

    private suspend fun syncLanguageAfterInitialLoad(normalizedLanguage: String) {
        refreshSearchHistory(normalizedLanguage)
        updateSearchStateForLanguage(normalizedLanguage)
        if (_state.value.searchSheetPage == FeedSearchSheetPage.Filters) {
            onCategorySearchTextChanged(_state.value.categorySearchText)
        }

        val current = _state.value
        val query = current.activeQuery.normalize()
        if (!hasLoadedInitialFeed ||
            current.contentState == FeedContentState.FirstSearch ||
            query.isEmpty
        ) {
            return
        }

        try {
            isLoadingFeed = true
            val response = feedApi.getFeed(normalizedLanguage, query, 0, PageSize, null)
            val snapshot = createSnapshot(normalizedLanguage, query, 0, response)
            if (_state.value.language != normalizedLanguage) {
                return
            }
            feedCacheRepository.writeLastSnapshot(snapshot)
            pendingRefreshSnapshot = null
            applySnapshot(snapshot)
        } catch (_: Throwable) {
            // Keep the current content resolved with the cached snapshot language.
        } finally {
            isLoadingFeed = false
        }
    }

    private fun updateSearchStateForLanguage(normalizedLanguage: String) {
        _state.update { current ->
            current.copy(
                language = normalizedLanguage,
                appliedFilters = current.appliedFilters.map {
                    it.localized(it.query.normalize(), normalizedLanguage, localizer, dateFormatter)
                },
                contentState = when (current.contentState) {
                    is FeedContentState.Error -> FeedContentState.Error(localizer.get("FeedLoadErrorMessage", normalizedLanguage))
                    else -> current.contentState
                },
                searchErrorText = current.searchErrorKey
                    ?.let { localizer.get(it, normalizedLanguage) }
                    .orEmpty()
            )
        }
    }

    private suspend fun applySnapshot(snapshot: FeedSnapshot) {
        favoriteRepository.refreshMetadata(snapshot.response.items)
        val favorites = favoriteRepository.current().map { it.id }.toSet()
        val query = snapshot.query.normalize()
        val items = snapshot.response.items.map { it.copy(isFavorited = it.id in favorites) }
        refreshSearchHistory(snapshot.language)
        _state.update {
            it.copy(
                language = snapshot.language,
                contentLanguage = snapshot.language,
                items = items,
                currentIndex = if (items.isEmpty()) -1 else 0,
                page = snapshot.page,
                activeQuery = query,
                contentState = if (items.isEmpty()) FeedContentState.Empty else FeedContentState.Ready,
                hasCompletedInitialCacheRead = true,
                pendingNewResultsCount = 0,
                shouldAutoOpenFirstSearch = false,
                showLocationRequiredError = false
            )
        }
    }

    private fun buildSearchQuery(state: FeedUiState = _state.value): FeedQuery {
        var query = FeedQuery(
            terms = state.searchTerms,
            attendanceScopeIds = state.selectedLocationScopes.map(FeedAttendanceScope::id),
            attendanceScopes = state.selectedLocationScopes,
            locations = state.customLocations
        )

        for (filter in state.appliedFilters) {
            query = query.merge(filter.query)
        }

        return query.normalize()
    }

    private fun setOptionalFilter(filter: FeedFilterChip) {
        updateDraft { current ->
            current.copy(
                appliedFilters = current.appliedFilters
                    .filterNot { it.kind == filter.kind }
                    .plus(filter),
                suggestedFilters = current.suggestedFilters.filterNot { it.kind == filter.kind }
            )
        }
    }

    private fun clearOptionalFilter(kind: FeedFilterKind) {
        updateDraft { current ->
            current.copy(appliedFilters = current.appliedFilters.filterNot { it.kind == kind })
        }
    }

    private fun createAppliedFilters(query: FeedQuery, language: String): List<FeedFilterChip> {
        val filters = mutableListOf<FeedFilterChip>()
        if (!query.category.isNullOrBlank()) {
            filters += FeedFilterChip(
                kind = FeedFilterKind.Category,
                label = categoryDisplayText(query.category, language, localizer),
                query = FeedQuery(category = query.category)
            )
        }

        if (!query.dateFrom.isNullOrBlank() || !query.dateTo.isNullOrBlank()) {
            filters += FeedFilterChip(
                kind = FeedFilterKind.DateRange,
                label = dateFormatter.formatDateRange(query.dateFrom, query.dateTo, language),
                query = FeedQuery(dateFrom = query.dateFrom, dateTo = query.dateTo)
            )
        }

        if (query.priceFilter != FeedPriceFilter.Any) {
            filters += FeedFilterChip(
                kind = FeedFilterKind.Price,
                label = priceDisplayText(query.priceFilter, language, localizer),
                query = FeedQuery(priceFilter = query.priceFilter)
            )
        }

        if (!query.accessibility.isNullOrBlank()) {
            filters += FeedFilterChip(
                kind = FeedFilterKind.Accessibility,
                label = accessibilityFilterDisplayText(query.accessibility, language, localizer),
                query = FeedQuery(accessibility = query.accessibility)
            )
        }

        if (query.accessibleForAge != null) {
            filters += ageFilter(query.accessibleForAge, language)
        }

        return filters
    }

    private fun applyAccessibleAgeFilter(age: Int) {
        updateDraft { current ->
            current.copy(
                accessibleAgeText = age.toString(),
                appliedFilters = current.appliedFilters
                    .filterNot { it.kind == FeedFilterKind.AgeRestriction }
                    .plus(ageFilter(age, current.language))
            )
        }
    }

    private fun ageFilter(age: Int, language: String): FeedFilterChip =
        FeedFilterChip(
            kind = FeedFilterKind.AgeRestriction,
            label = accessibleForAgeDisplayText(age, language, localizer),
            query = FeedQuery(accessibleForAge = age)
        )

    private fun createSnapshot(
        language: String,
        query: FeedQuery,
        page: Int,
        response: FeedPageResponse
    ): FeedSnapshot =
        FeedSnapshot(
            language = normalizeAppLanguageTag(language),
            query = query.normalize(),
            page = page,
            pageSize = PageSize,
            response = response,
            cachedAt = OffsetDateTime.now().toString()
        )

    private suspend fun saveSearchHistory(query: FeedQuery) {
        val normalized = query.normalize()
        if (normalized.isEmpty) {
            return
        }

        searchHistoryRepository.save(
            FeedSearchHistoryEntry(
                query = normalized,
                attendanceScopeId = normalized.attendanceScopeId,
                attendanceScope = normalized.attendanceScope,
                text = normalized.text,
                filters = _state.value.appliedFilters.filter { it.kind != FeedFilterKind.Location },
                lastUsedAt = OffsetDateTime.now().toString()
            )
        )
        refreshSearchHistory(_state.value.language)
    }

    private suspend fun refreshSearchHistory(language: String) {
        val history = searchHistoryRepository.read()
            .sortedByDescending { it.lastUsedAt }
            .map { it.toHistoryItem(language) }

        _state.update { it.copy(searchHistory = history) }
    }

    private fun FeedSearchHistoryEntry.toHistoryItem(language: String): FeedSearchHistoryItem {
        val normalizedQuery = query?.normalize() ?: return FeedSearchHistoryItem(this, "", "")
        val parts = buildList {
            addAll(normalizedQuery.terms)
            addAll(normalizedQuery.attendanceScopes.map(FeedAttendanceScope::displayPath))
            addAll(normalizedQuery.locations)
            addAll(createAppliedFilters(normalizedQuery, language).map(FeedFilterChip::label))
        }.filter(String::isNotBlank)
        val title = parts.firstOrNull() ?: localizer.get("FindEvents", language)

        return FeedSearchHistoryItem(
            entry = copy(query = normalizedQuery),
            title = title,
            subtitle = parts.drop(1).joinToString(" · ")
        )
    }

    private fun addSearchTerm(value: String) {
        val term = value.trim()
        if (term.length < 2) {
            return
        }
        termSearchJob?.cancel()
        updateDraft { current ->
            current.copy(
                searchTerms = (current.searchTerms + term).distinctBy(String::lowercase),
                termSearchText = "",
                termSuggestions = emptyList(),
                isTermSearchLoading = false,
                termSearchErrorText = ""
            )
        }
    }

    private fun applySearchDraft(query: FeedQuery?) {
        val normalized = query?.normalize()?.takeUnless { it.isEmpty }
        _state.update { current ->
            current.copy(
                searchTerms = normalized?.terms.orEmpty(),
                selectedLocationScopes = normalized?.attendanceScopes.orEmpty(),
                customLocations = normalized?.locations.orEmpty(),
                termSearchText = "",
                termSuggestions = emptyList(),
                isTermSearchLoading = false,
                termSearchErrorText = "",
                searchText = "",
                parsedSearchText = null,
                selectedLocationScope = null,
                locationSearchText = "",
                locationSuggestions = emptyList(),
                isLocationSearchLoading = false,
                locationSearchErrorText = "",
                suggestedFilters = emptyList(),
                appliedFilters = normalized
                    ?.let { createAppliedFilters(it, current.language) }
                    .orEmpty(),
                searchSheetPage = FeedSearchSheetPage.Search,
                categorySearchText = "",
                accessibilitySearchText = "",
                accessibleAgeText = "",
                categoryOptions = emptyList(),
                isCategorySearchLoading = false,
                categorySearchErrorText = "",
                showLocationRequiredError = false,
                searchErrorKey = null,
                searchErrorText = ""
            )
        }
    }

    private fun updateDraft(transform: (FeedUiState) -> FeedUiState) {
        _state.update { current ->
            transform(current).copy(
                searchErrorKey = null,
                searchErrorText = "",
                showLocationRequiredError = false
            )
        }
        persistDraft()
    }

    private fun persistDraft() {
        val query = buildSearchQuery()
        draftPersistenceJob?.cancel()
        draftPersistenceJob = viewModelScope.launch {
            if (query.isEmpty) {
                searchDraftRepository.clear()
            } else {
                searchDraftRepository.save(query)
            }
        }
    }

    private suspend fun flushDraft(query: FeedQuery) {
        draftPersistenceJob?.cancelAndJoin()
        searchDraftRepository.save(query)
    }

    private fun FeedFilterChip.sameFilter(other: FeedFilterChip): Boolean =
        kind == other.kind && query.normalize() == other.query.normalize()

    private companion object {
        const val PageSize = 10
    }
}

sealed interface FeedEvent {
    data object SearchApplied : FeedEvent
}

private object EmptySearchDraftStore : SearchDraftStore {
    override suspend fun read() = null

    override suspend fun save(query: FeedQuery) = Unit

    override suspend fun clear() = Unit
}
