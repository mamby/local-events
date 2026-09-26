package net.mamby.events.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamby.events.core.di.ApplicationScope

val Context.localEventsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fralov")

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    private val appLocaleController: AppLocaleController
) : SettingsStore {
    private val localeRefresh = MutableStateFlow(0)
    private val opacityPreview = MutableStateFlow<Float?>(null)

    override val settings: Flow<SettingsState> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .combine(localeRefresh) { preferences, _ -> preferences.toSettingsState() }
            .combine(opacityPreview) { settings, preview ->
                settings.copy(floatingSurfaceOpacityLevel = preview ?: settings.floatingSurfaceOpacityLevel)
            }

    override suspend fun current(): SettingsState =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it.toSettingsState() }
            .first()

    override suspend fun setLanguageTag(languageTag: String?) {
        appLocaleController.setSelectedLanguageTag(languageTag)
        dataStore.edit { it.remove(LanguageKey) }
        refreshLocaleState()
    }

    override suspend fun migrateLegacyLanguagePreference() {
        if (appLocaleController.selectedLanguageTag() != null) {
            dataStore.edit { it.remove(LanguageKey) }
            refreshLocaleState()
            return
        }

        val legacyPreference = dataStore.data.first()[LanguageKey].toLanguagePreference()
        val legacyLanguageTag = legacyPreference.languageTagOrNull()
        if (legacyLanguageTag != null) {
            appLocaleController.setSelectedLanguageTag(legacyLanguageTag)
        }

        dataStore.edit { it.remove(LanguageKey) }
        refreshLocaleState()
    }

    override fun refreshLocaleState() {
        localeRefresh.update { it + 1 }
    }

    override fun previewFloatingSurfaceOpacityLevel(level: Float) {
        opacityPreview.value = normalizeFloatingSurfaceOpacityLevel(level)
    }

    override suspend fun saveFloatingSurfaceOpacityLevel() {
        val normalized = opacityPreview.value ?: return
        dataStore.edit { it[OpacityKey] = normalized }
        opacityPreview.compareAndSet(normalized, null)
    }

    override suspend fun setThemePreference(preference: AppThemePreference) {
        dataStore.edit { it[ThemeKey] = preference.name }
    }

    override suspend fun setMediaAutoplayEnabled(enabled: Boolean) {
        dataStore.edit { it[MediaAutoplayKey] = enabled }
    }

    override suspend fun setHouseholdChildAges(ages: List<Int>) {
        require(ages.all { it in ValidAgeRange }) { "Household child ages must be between 0 and 120." }
        dataStore.edit { it[HouseholdChildAgesKey] = json.encodeToString(ages) }
    }

    private fun String?.toLanguagePreference(): AppLanguagePreference =
        enumValueOrDefault(this, AppLanguagePreference.System)

    private fun String?.toThemePreference(): AppThemePreference =
        enumValueOrDefault(this, AppThemePreference.System)

    private fun AppLanguagePreference.languageTagOrNull(): String? =
        when (this) {
            AppLanguagePreference.English -> "en"
            AppLanguagePreference.French -> "fr"
            AppLanguagePreference.Arabic -> "ar"
            AppLanguagePreference.System -> null
        }

    private fun Preferences.toSettingsState(): SettingsState {
        val theme = this[ThemeKey].toThemePreference()
        return SettingsState(
            selectedLanguageTag = appLocaleController.selectedLanguageTag(),
            themePreference = theme,
            floatingSurfaceOpacityLevel = normalizeFloatingSurfaceOpacityLevel(this[OpacityKey] ?: 0f),
            mediaAutoplayEnabled = this[MediaAutoplayKey]
                ?: this[LegacyAudioAutoplayKey]
                ?: false,
            householdChildAges = this[HouseholdChildAgesKey]
                ?.decodeOrNull<List<Int>>(json)
                .orEmpty()
                .filter { it in ValidAgeRange },
            systemLanguageTag = appLocaleController.systemLanguageTag(),
            effectiveLanguage = appLocaleController.effectiveLanguageTag()
        )
    }

    private companion object {
        val LanguageKey = stringPreferencesKey("settings.language")
        val OpacityKey = floatPreferencesKey("settings.floatingSurfaceOpacityLevel")
        val ThemeKey = stringPreferencesKey("settings.theme")
        val MediaAutoplayKey = booleanPreferencesKey("settings.mediaAutoplayEnabled")
        val LegacyAudioAutoplayKey = booleanPreferencesKey("settings.audioAutoplayEnabled")
        val HouseholdChildAgesKey = stringPreferencesKey("settings.householdChildAges")
        val ValidAgeRange = 0..120
    }
}

interface SettingsStore {
    val settings: Flow<SettingsState>

    suspend fun current(): SettingsState

    suspend fun setLanguageTag(languageTag: String?)

    suspend fun migrateLegacyLanguagePreference()

    fun refreshLocaleState()

    fun previewFloatingSurfaceOpacityLevel(level: Float)

    suspend fun saveFloatingSurfaceOpacityLevel()

    suspend fun setThemePreference(preference: AppThemePreference)

    suspend fun setMediaAutoplayEnabled(enabled: Boolean)

    suspend fun setHouseholdChildAges(ages: List<Int>)
}

@Singleton
class FeedApi @Inject constructor(
    private val httpClient: HttpClient
) : FeedRemoteDataSource {
    override suspend fun getFeed(
        language: String,
        query: FeedQuery,
        page: Int,
        pageSize: Int,
        knownCursor: String?
    ): FeedPageResponse {
        val normalized = query.normalize()
        val response = httpClient.get("v1/feed") {
            parameter("lang", normalizeAppLanguageTag(language))
            parameter("page", page)
            parameter("pageSize", pageSize)
            knownCursor?.takeIf(String::isNotBlank)?.let { parameter("knownCursor", it) }
            normalized.terms.forEach { parameter("terms", it) }
            normalized.attendanceScopeIds.forEach { parameter("attendanceScopeIds", it) }
            normalized.locations.forEach { parameter("locations", it) }
            normalized.category?.let { parameter("category", it) }
            normalized.accessibility?.let { parameter("accessibility", it) }
            normalized.accessibleForAge?.let { parameter("accessibleForAge", it) }
            normalized.dateFrom?.let { parameter("dateFrom", it) }
            normalized.dateTo?.let { parameter("dateTo", it) }
            if (normalized.priceFilter != FeedPriceFilter.Any) {
                parameter("priceFilter", normalized.priceFilter.name)
            }
        }
        check(response.status == HttpStatusCode.OK) {
            "Feed request failed with HTTP ${response.status.value}."
        }
        return response.body()
    }

    override suspend fun searchAttendanceScopes(text: String, language: String, limit: Int): List<FeedAttendanceScope> {
        if (text.isBlank()) {
            return emptyList()
        }

        return httpClient.get("v1/feed/attendance-scopes/search") {
            parameter("lang", normalizeAppLanguageTag(language))
            parameter("text", text.trim())
            parameter("limit", limit)
        }.body<List<FeedAttendanceScope>>()
            .asSequence()
            .map(FeedAttendanceScope::normalize)
            .filter { it.id.isNotBlank() && it.isSearchable }
            .distinctBy { it.id.lowercase() }
            .toList()
    }

    override suspend fun searchCategories(text: String, language: String, limit: Int): List<FeedCategoryOption> =
        httpClient.get("v1/feed/categories/search") {
            parameter("lang", normalizeAppLanguageTag(language))
            parameter("text", text.trim())
            parameter("limit", limit)
        }.body<List<FeedCategoryOption>>()
            .asSequence()
            .map(FeedCategoryOption::normalize)
            .filter { it.value.isNotBlank() && it.label.isNotBlank() }
            .distinctBy { it.value.lowercase() }
            .toList()

    override suspend fun searchTerms(text: String, language: String, limit: Int): List<FeedTermOption> {
        if (text.isBlank()) {
            return emptyList()
        }

        val response = httpClient.get("v1/feed/terms/search") {
            parameter("lang", normalizeAppLanguageTag(language))
            parameter("text", text.trim())
            parameter("limit", limit)
        }
        check(response.status == HttpStatusCode.OK) {
            "Term search failed with HTTP ${response.status.value}."
        }
        return response.body<List<FeedTermOption>>()
            .asSequence()
            .map(FeedTermOption::normalize)
            .filter { it.value.isNotBlank() && it.label.isNotBlank() }
            .distinctBy { it.value.lowercase() }
            .toList()
    }
}

interface FeedRemoteDataSource {
    suspend fun getFeed(
        language: String,
        query: FeedQuery,
        page: Int,
        pageSize: Int,
        knownCursor: String?
    ): FeedPageResponse

    suspend fun searchAttendanceScopes(text: String, language: String, limit: Int = 8): List<FeedAttendanceScope>

    suspend fun searchCategories(text: String, language: String, limit: Int = 8): List<FeedCategoryOption>

    suspend fun searchTerms(text: String, language: String, limit: Int = 8): List<FeedTermOption> = emptyList()
}

@Singleton
class TelemetryApi @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun createSession(): TelemetrySessionResponse =
        httpClient.post("v1/telemetry/sessions").body()

    suspend fun sendInteraction(
        sessionToken: String,
        request: TelemetryInteractionRequest
    ): HttpStatusCode =
        httpClient.post("v1/telemetry/interactions") {
            header(TelemetrySessionHeader, sessionToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.status

    private companion object {
        const val TelemetrySessionHeader = "X-Fralov-Telemetry-Session"
    }
}

@Singleton
class TelemetrySessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val telemetryApi: TelemetryApi
) {
    suspend fun getOrCreateToken(): String =
        currentToken() ?: createAndStoreToken()

    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(SessionTokenKey)
        }
    }

    private suspend fun currentToken(): String? =
        dataStore.data.first()[SessionTokenKey]
            ?.trim()
            ?.takeIf(String::isNotBlank)

    private suspend fun createAndStoreToken(): String {
        val sessionToken = telemetryApi.createSession().sessionToken.trim()
        require(sessionToken.isNotBlank()) { "Telemetry session token must not be blank." }

        dataStore.edit { preferences ->
            preferences[SessionTokenKey] = sessionToken
        }

        return sessionToken
    }

    private companion object {
        val SessionTokenKey = stringPreferencesKey("telemetry.sessionToken")
    }
}

@Singleton
class TelemetryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    private val sessionRepository: TelemetrySessionRepository,
    private val telemetryApi: TelemetryApi
) : TelemetryRecorder {
    private val drainMutex = Mutex()
    private var drainJob: Job? = null

    override fun start() {
        requestDrain()
    }

    override fun stop() {
        drainJob?.cancel()
        drainJob = null
    }

    override suspend fun recordView(eventId: String) {
        enqueue(eventId, TelemetryInteractionAction.View)
    }

    override suspend fun recordFavorite(eventId: String, isFavorited: Boolean) {
        enqueue(
            eventId,
            if (isFavorited) TelemetryInteractionAction.FavoriteAdded else TelemetryInteractionAction.FavoriteRemoved
        )
    }

    override suspend fun recordShareIntent(eventId: String) {
        enqueue(eventId, TelemetryInteractionAction.ShareIntent)
    }

    private suspend fun enqueue(eventId: String, action: TelemetryInteractionAction) {
        if (eventId.isBlank()) {
            return
        }

        dataStore.edit { preferences ->
            val queued = preferences[QueueKey]
                ?.decodeOrNull<List<QueuedTelemetryInteraction>>(json)
                .orEmpty()
                .plus(
                    QueuedTelemetryInteraction(
                        eventId = eventId,
                        action = action,
                        enqueuedAt = OffsetDateTime.now().toString()
                    )
                )
                .takeLast(MaximumQueuedInteractions)

            preferences[QueueKey] = json.encodeToString(queued)
        }

        requestDrain()
    }

    private fun requestDrain() {
        if (drainJob?.isActive == true) {
            return
        }

        drainJob = applicationScope.launch {
            drainQueue()
        }
    }

    private suspend fun drainQueue() {
        drainMutex.withLock {
            var retryDelayMillis = InitialRetryDelayMillis

            while (true) {
                val item = nextQueuedInteraction() ?: return
                val result = runCatching { sendWithSessionRetry(item.toRequest()) }.getOrDefault(SendResult.Retry)

                when (result) {
                    SendResult.Delivered,
                    SendResult.PermanentFailure -> {
                        removeQueuedInteraction(item.id)
                        retryDelayMillis = InitialRetryDelayMillis
                    }

                    SendResult.Retry -> {
                        delay(retryDelayMillis)
                        retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(MaximumRetryDelayMillis)
                    }
                }
            }
        }
    }

    private suspend fun nextQueuedInteraction(): QueuedTelemetryInteraction? =
        dataStore.data.first()[QueueKey]
            ?.decodeOrNull<List<QueuedTelemetryInteraction>>(json)
            .orEmpty()
            .firstOrNull()

    private suspend fun removeQueuedInteraction(id: String) {
        dataStore.edit { preferences ->
            val remaining = preferences[QueueKey]
                ?.decodeOrNull<List<QueuedTelemetryInteraction>>(json)
                .orEmpty()
                .filterNot { it.id == id }

            if (remaining.isEmpty()) {
                preferences.remove(QueueKey)
            } else {
                preferences[QueueKey] = json.encodeToString(remaining)
            }
        }
    }

    private suspend fun sendWithSessionRetry(request: TelemetryInteractionRequest): SendResult {
        val sessionToken = sessionRepository.getOrCreateToken()
        val status = telemetryApi.sendInteraction(sessionToken, request)

        if (status == HttpStatusCode.Unauthorized) {
            sessionRepository.clearToken()
            return telemetryApi.sendInteraction(sessionRepository.getOrCreateToken(), request).toSendResult()
        }

        return status.toSendResult()
    }

    private fun HttpStatusCode.toSendResult(): SendResult =
        when (this) {
            HttpStatusCode.NoContent,
            HttpStatusCode.OK,
            HttpStatusCode.Accepted -> SendResult.Delivered

            HttpStatusCode.BadRequest -> SendResult.PermanentFailure
            HttpStatusCode.TooManyRequests -> SendResult.Retry
            else -> if (value in 400..499) SendResult.PermanentFailure else SendResult.Retry
        }

    private enum class SendResult {
        Delivered,
        PermanentFailure,
        Retry
    }

    private companion object {
        const val MaximumQueuedInteractions = 500
        const val InitialRetryDelayMillis = 15_000L
        const val MaximumRetryDelayMillis = 300_000L
        val QueueKey = stringPreferencesKey("telemetry.interactionQueue")
    }
}

interface TelemetryRecorder {
    fun start()

    fun stop()

    suspend fun recordView(eventId: String)

    suspend fun recordFavorite(eventId: String, isFavorited: Boolean)

    suspend fun recordShareIntent(eventId: String)
}

@Singleton
class FeedCacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : FeedCacheStore {
    override suspend fun readLastSnapshot(): FeedSnapshot? {
        val entry = dataStore.data.first()[CacheKey]
            ?.decodeOrNull<FeedSnapshotCacheEntry>(json)
            ?: return null

        val snapshot = entry.snapshot
        if (entry.version != CurrentCacheVersion ||
            snapshot.response.items.isEmpty() ||
            !hasUsableQuery(snapshot.query)
        ) {
            return null
        }

        return snapshot.copy(query = snapshot.query.normalize())
    }

    override suspend fun writeLastSnapshot(snapshot: FeedSnapshot) {
        if (snapshot.response.items.isEmpty()) {
            dataStore.edit { it.remove(CacheKey) }
            return
        }
        if (!hasUsableQuery(snapshot.query)) {
            return
        }

        val normalized = snapshot.copy(
            language = normalizeAppLanguageTag(snapshot.language),
            query = snapshot.query.normalize(),
            cachedAt = OffsetDateTime.now().toString()
        )

        val entry = FeedSnapshotCacheEntry(CurrentCacheVersion, normalized)
        dataStore.edit { it[CacheKey] = json.encodeToString(entry) }
    }

    private fun hasUsableQuery(query: FeedQuery): Boolean {
        val normalized = query.normalize()
        if (normalized.isEmpty) {
            return false
        }

        val searchableScopeIds = normalized.attendanceScopes
            .filter(FeedAttendanceScope::isSearchable)
            .map(FeedAttendanceScope::id)
            .toSet()
        return normalized.attendanceScopeIds.all { scopeId ->
            searchableScopeIds.any { it.equals(scopeId, ignoreCase = true) }
        }
    }

    private companion object {
        const val CurrentCacheVersion = 7
        val CacheKey = stringPreferencesKey("feed-cache.last-search")
    }
}

interface FeedCacheStore {
    suspend fun readLastSnapshot(): FeedSnapshot?

    suspend fun writeLastSnapshot(snapshot: FeedSnapshot)
}

@Singleton
class FavoritesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : FavoritesStore {
    override val favorites: Flow<List<EventFeedItem>> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                preferences[FavoritesKey]
                    ?.decodeOrNull<List<EventFeedItem>>(json)
                    .orEmpty()
                    .normalizeFavorites()
            }

    override suspend fun current(): List<EventFeedItem> = favorites.first()

    override suspend fun setFavorite(item: EventFeedItem, isFavorited: Boolean) {
        val current = current().toMutableList()
        val existingIndex = current.indexOfFirst { it.isSameEvent(item) }

        if (!isFavorited) {
            if (existingIndex < 0) {
                return
            }

            current.removeAt(existingIndex)
        } else {
            val favorite = item.copy(isFavorited = true)
            if (existingIndex >= 0) {
                current[existingIndex] = favorite
            } else {
                current.add(0, favorite)
            }
        }

        dataStore.edit { it[FavoritesKey] = json.encodeToString(current.normalizeFavorites()) }
    }

    override suspend fun refreshMetadata(items: List<EventFeedItem>) {
        if (items.isEmpty()) {
            return
        }

        val current = current()
        val refreshed = current.map { favorite ->
            items.firstOrNull { it.isSameEvent(favorite) }?.copy(isFavorited = true) ?: favorite
        }

        if (refreshed != current) {
            dataStore.edit { it[FavoritesKey] = json.encodeToString(refreshed.normalizeFavorites()) }
        }
    }

    private fun List<EventFeedItem>.normalizeFavorites(): List<EventFeedItem> {
        val normalized = mutableListOf<EventFeedItem>()
        forEach { favorite ->
            if (normalized.none { it.isSameEvent(favorite) }) {
                normalized += favorite.copy(isFavorited = true)
            }
        }
        return normalized
    }

    private fun EventFeedItem.isSameEvent(other: EventFeedItem): Boolean =
        id == other.id || sequence == other.sequence && title.default == other.title.default

    private companion object {
        val FavoritesKey = stringPreferencesKey("favorite-events")
    }
}

interface FavoritesStore {
    val favorites: Flow<List<EventFeedItem>>

    suspend fun current(): List<EventFeedItem>

    suspend fun setFavorite(item: EventFeedItem, isFavorited: Boolean)

    suspend fun refreshMetadata(items: List<EventFeedItem>)
}

@Singleton
class SearchHistoryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : SearchHistoryStore {
    override suspend fun read(): List<FeedSearchHistoryEntry> =
        dataStore.data.first()[SearchHistoryKey]
            ?.decodeOrNull<List<FeedSearchHistoryEntry>>(json)
            .orEmpty()
            .mapNotNull { it.normalizeOrNull() }

    override suspend fun save(entry: FeedSearchHistoryEntry) {
        val normalized = entry.normalizeOrNull() ?: return
        val entries = read()
            .filterNot { it.isSameSearch(normalized) }
            .plus(normalized)
            .sortedByDescending { it.lastUsedAt }
            .take(MaximumHistoryEntries)

        dataStore.edit { it[SearchHistoryKey] = json.encodeToString(entries) }
    }

    override suspend fun remove(entry: FeedSearchHistoryEntry) {
        val normalized = entry.normalizeOrNull() ?: return
        val entries = read().filterNot { it.isSameSearch(normalized) }
        dataStore.edit {
            if (entries.isEmpty()) {
                it.remove(SearchHistoryKey)
            } else {
                it[SearchHistoryKey] = json.encodeToString(entries)
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(SearchHistoryKey) }
    }

    private fun FeedSearchHistoryEntry.normalizeOrNull(): FeedSearchHistoryEntry? {
        var normalizedQuery = (
            query ?: FeedQuery(
                attendanceScopeId = attendanceScopeId,
                attendanceScope = attendanceScope,
                locationScopeId = locationScopeId,
                locationScope = locationScope,
                text = text
            )
            ).normalize()
        filters
            .filter { it.kind != FeedFilterKind.Location }
            .forEach { filter -> normalizedQuery = normalizedQuery.merge(filter.query) }
        if (normalizedQuery.isEmpty) {
            return null
        }

        return copy(
            query = normalizedQuery,
            attendanceScopeId = normalizedQuery.attendanceScopeId,
            attendanceScope = normalizedQuery.attendanceScope,
            locationScopeId = "",
            locationScope = null,
            text = normalizedQuery.text,
            filters = filters
                .filter { it.kind != FeedFilterKind.Location }
                .distinctBy { it.filterKey() }
                .sortedWith(compareBy<FeedFilterChip> { it.kind.name }.thenBy { it.filterKey() })
        )
    }

    private fun FeedSearchHistoryEntry.isSameSearch(other: FeedSearchHistoryEntry): Boolean =
        query?.searchKey() == other.query?.searchKey()

    private fun FeedFilterChip.filterKey(): String {
        val normalized = query.normalize()
        return listOf(
            kind.name,
            normalized.category.orEmpty(),
            normalized.accessibility.orEmpty(),
            normalized.accessibleForAge?.toString().orEmpty(),
            normalized.dateFrom.orEmpty(),
            normalized.dateTo.orEmpty(),
            normalized.priceFilter.name
        ).joinToString(":")
    }

    private fun FeedQuery.searchKey(): String {
        val normalized = normalize()
        return listOf(
            normalized.terms.sortedBy(String::lowercase).joinToString("|"),
            normalized.attendanceScopeIds.sorted().joinToString("|"),
            normalized.locations.sortedBy(String::lowercase).joinToString("|"),
            normalized.category.orEmpty().lowercase(),
            normalized.accessibility.orEmpty().lowercase(),
            normalized.accessibleForAge?.toString().orEmpty(),
            normalized.dateFrom.orEmpty(),
            normalized.dateTo.orEmpty(),
            normalized.priceFilter.name
        ).joinToString("::")
    }

    private companion object {
        const val MaximumHistoryEntries = 10
        val SearchHistoryKey = stringPreferencesKey("feed-search-history")
    }
}

interface SearchHistoryStore {
    suspend fun read(): List<FeedSearchHistoryEntry>

    suspend fun save(entry: FeedSearchHistoryEntry)

    suspend fun remove(entry: FeedSearchHistoryEntry) = Unit

    suspend fun clear() = Unit
}

@Singleton
class SearchDraftRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : SearchDraftStore {
    override suspend fun read(): FeedSearchDraft? =
        dataStore.data.first()[SearchDraftKey]
            ?.decodeOrNull<FeedSearchDraft>(json)
            ?.let { draft ->
                val query = draft.query.normalize()
                draft.copy(query = query).takeUnless { query.isEmpty }
            }

    override suspend fun save(query: FeedQuery) {
        val normalized = query.normalize()
        if (normalized.isEmpty) {
            clear()
            return
        }

        val draft = FeedSearchDraft(
            query = normalized,
            savedAt = OffsetDateTime.now().toString()
        )
        dataStore.edit { it[SearchDraftKey] = json.encodeToString(draft) }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(SearchDraftKey) }
    }

    private companion object {
        val SearchDraftKey = stringPreferencesKey("feed-search-draft")
    }
}

interface SearchDraftStore {
    suspend fun read(): FeedSearchDraft?

    suspend fun save(query: FeedQuery)

    suspend fun clear()
}

private inline fun <reified T> String.decodeOrNull(json: Json): T? =
    try {
        json.decodeFromString<T>(this)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: fallback
