package net.mamby.events

import java.time.LocalDate
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.mamby.events.core.AppTextProvider
import net.mamby.events.core.AppThemePreference
import net.mamby.events.core.DateTextFormatter
import net.mamby.events.core.EventAttendanceOption
import net.mamby.events.core.EventAttendanceType
import net.mamby.events.core.EventFeedItem
import net.mamby.events.core.EventOccurrence
import net.mamby.events.core.EventPriceOption
import net.mamby.events.core.EventPriceType
import net.mamby.events.core.EventSource
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
import net.mamby.events.core.LocalizedText
import net.mamby.events.core.Media
import net.mamby.events.core.MediaType
import net.mamby.events.core.SearchHistoryStore
import net.mamby.events.core.SettingsState
import net.mamby.events.core.SettingsStore
import net.mamby.events.core.TelemetryRecorder
import net.mamby.events.features.feed.FeedScreen
import net.mamby.events.features.feed.FeedLoadingMorePillTestTag
import net.mamby.events.features.feed.FeedNewItemsPillTestTag
import net.mamby.events.features.feed.FeedSearchPillTestTag
import net.mamby.events.features.feed.FeedViewModel
import net.mamby.events.ui.LocalEventsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeedScreenBehaviorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun favoriteLongPress_opensFavorites_withoutTogglingFavoriteOrTelemetry() {
        val dependencies = FeedScreenTestDependencies()
        var favoritesOpenCount = 0
        renderFeed(dependencies.viewModel) { favoritesOpenCount++ }

        favoriteButton()
            .performTouchInput { longClick() }
        composeRule.waitForIdle()

        assertEquals(1, favoritesOpenCount)
        assertEquals(emptyList<EventFeedItem>(), dependencies.favoritesStore.items)
        assertEquals(emptyList<Pair<String, Boolean>>(), dependencies.telemetryRecorder.favoriteEvents)
    }

    @Test
    fun favoriteTap_togglesFavoriteAndTelemetry_withoutOpeningFavorites() {
        val dependencies = FeedScreenTestDependencies()
        var favoritesOpenCount = 0
        renderFeed(dependencies.viewModel) { favoritesOpenCount++ }

        favoriteButton().performClick()
        composeRule.waitForIdle()

        assertEquals(0, favoritesOpenCount)
        assertEquals(listOf(dependencies.item.copy(isFavorited = true)), dependencies.favoritesStore.items)
        assertEquals(listOf(dependencies.item.id to true), dependencies.telemetryRecorder.favoriteEvents)
    }

    @Test
    fun menuFavorites_opensFavorites_withoutChangingFavoriteOrTelemetry() {
        val dependencies = FeedScreenTestDependencies()
        var favoritesOpenCount = 0
        renderFeed(dependencies.viewModel) { favoritesOpenCount++ }

        menuButton().performClick()
        composeRule.onNodeWithText("FavoritesTitle").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(1, favoritesOpenCount)
        assertEquals(emptyList<EventFeedItem>(), dependencies.favoritesStore.items)
        assertEquals(emptyList<Pair<String, Boolean>>(), dependencies.telemetryRecorder.favoriteEvents)
    }

    @Test
    fun menuSettings_opensSettings() {
        val dependencies = FeedScreenTestDependencies()
        var settingsOpenCount = 0
        renderFeed(dependencies.viewModel, onOpenFavorites = {}, onOpenSettings = { settingsOpenCount++ })

        menuButton().performClick()
        composeRule.onNodeWithText("SettingsTitle").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(1, settingsOpenCount)
    }

    @Test
    fun mediaLongPress_opensDetailsSheet() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel) {}

        mediaBackground().performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("EventDetailsDescription").assertIsDisplayed()
    }

    @Test
    fun customDatePicker_appliesSelection_and_dismiss_preservesAppliedRange() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel) {}
        val startDate = LocalDate.of(2026, 4, 3)
        val endDate = LocalDate.of(2026, 4, 9)
        val dateLabel = "2026-04-03-2026-04-09"

        composeRule.onNodeWithTag(FeedSearchPillTestTag).performClick()
        composeRule.onNodeWithText("EditFilters").performClick()
        composeRule.onNodeWithText("ChooseDates").assertIsDisplayed()
        composeRule.runOnIdle {
            dependencies.viewModel.selectCustomDateRange(startDate, endDate)
        }
        composeRule.onAllNodesWithText(dateLabel, useUnmergedTree = true)[1].performClick()
        composeRule.onNodeWithText("ChooseDatesTitle").assertIsDisplayed()
        composeRule.onNodeWithText("Apply").performClick()

        composeRule.runOnIdle {
            val appliedDate = dependencies.viewModel.state.value.appliedFilters.single {
                it.kind == FeedFilterKind.DateRange
            }
            assertEquals("2026-04-03", appliedDate.query.dateFrom)
            assertEquals("2026-04-09", appliedDate.query.dateTo)
        }

        composeRule.onAllNodesWithText(dateLabel, useUnmergedTree = true)[1].performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.runOnIdle {
            val appliedDate = dependencies.viewModel.state.value.appliedFilters.single {
                it.kind == FeedFilterKind.DateRange
            }
            assertEquals("2026-04-03", appliedDate.query.dateFrom)
            assertEquals("2026-04-09", appliedDate.query.dateTo)
        }
    }

    @Test
    fun searchSheet_keepsFirstControlBelowSheetChrome() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel) {}

        composeRule.onNodeWithTag(FeedSearchPillTestTag).performClick()

        val chromeBottom = composeRule.onNodeWithContentDescription("Close")
            .fetchSemanticsNode()
            .boundsInRoot
            .bottom
        val firstControlTop = composeRule.onNodeWithText("SearchTermsLabel")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue(firstControlTop >= chromeBottom)
    }

    @Test
    fun attendanceSummary_keepsAdditionalOptionCount_visibleForLongPlace() {
        val item = feedScreenTestItem(
            attendanceOptions = listOf(
                physicalAttendance("Issy-les-Moulineaux", "issy-les-moulineaux"),
                EventAttendanceOption(type = EventAttendanceType.Online),
                EventAttendanceOption(type = EventAttendanceType.Television)
            )
        )
        val dependencies = FeedScreenTestDependencies(items = listOf(item))

        renderFeed(dependencies.viewModel) {}

        composeRule.onAllNodesWithText("+2", useUnmergedTree = true)[0].assertIsDisplayed()
    }

    @Test
    fun feedTeaser_showsSimpleFreePrice() {
        assertFeedTeaserPrice(
            priceOptions = listOf(EventPriceOption(type = EventPriceType.Free)),
            expectedPrice = "Free",
            expectsAdditionalDetails = false
        )
    }

    @Test
    fun feedTeaser_showsSimplePaidPrice() {
        assertFeedTeaserPrice(
            priceOptions = listOf(EventPriceOption(type = EventPriceType.Paid, amountMinorUnits = 1800, currencyCode = "EUR")),
            expectedPrice = "€ 18",
            expectsAdditionalDetails = false
        )
    }

    @Test
    fun feedTeaser_showsAdditionalPaidPriceIndicator() {
        assertFeedTeaserPrice(
            priceOptions = listOf(
                EventPriceOption(type = EventPriceType.Paid, amountMinorUnits = 1800, currencyCode = "EUR"),
                EventPriceOption(type = EventPriceType.Paid, amountMinorUnits = 2500, currencyCode = "EUR")
            ),
            expectedPrice = "€ 18",
            expectsAdditionalDetails = true
        )
    }

    @Test
    fun feedTeaser_showsConditionalFreePriceIndicator() {
        assertFeedTeaserPrice(
            priceOptions = listOf(
                EventPriceOption(
                    type = EventPriceType.Free,
                    condition = LocalizedText(default = "Children under 12")
                ),
                EventPriceOption(type = EventPriceType.Paid, amountMinorUnits = 1800, currencyCode = "EUR")
            ),
            expectedPrice = "Free",
            expectsAdditionalDetails = true
        )
    }

    @Test
    fun onlineOnlyAttendance_showsOnlineLabel() {
        val online = feedScreenTestItem(
            attendanceOptions = listOf(EventAttendanceOption(type = EventAttendanceType.Online, isPrimary = true))
        )
        renderFeed(FeedScreenTestDependencies(items = listOf(online)).viewModel) {}
        composeRule.onAllNodesWithText("Online", useUnmergedTree = true)[0].assertIsDisplayed()
    }

    @Test
    fun televisionOnlyAttendance_showsTelevisionLabel() {
        val television = feedScreenTestItem(
            attendanceOptions = listOf(EventAttendanceOption(type = EventAttendanceType.Television, isPrimary = true))
        )
        renderFeed(FeedScreenTestDependencies(items = listOf(television)).viewModel) {}
        composeRule.onAllNodesWithText("TV", useUnmergedTree = true)[0].assertIsDisplayed()
    }

    @Test
    fun details_useLongDescription_andListAttendanceOptions() {
        val item = feedScreenTestItem(
            longDescription = "Expanded event description.",
            attendanceOptions = listOf(
                physicalAttendance("Station F, Paris", "paris"),
                EventAttendanceOption(type = EventAttendanceType.Online),
                EventAttendanceOption(
                    type = EventAttendanceType.Television,
                    displayName = LocalizedText(default = "Arte")
                )
            )
        )
        val dependencies = FeedScreenTestDependencies(items = listOf(item))
        renderFeed(dependencies.viewModel) {}

        mediaBackground().performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Expanded event description.").assertIsDisplayed()
        composeRule.onNodeWithText("Online").assertIsDisplayed()
        composeRule.onNodeWithText("TV: Arte").assertIsDisplayed()
    }

    @Test
    fun details_expandOccurrences_andPriceConditions() {
        val item = feedScreenTestItem(
            occurrences = listOf(
                EventOccurrence(startsAt = TestDate, endsAt = "2026-05-23T02:00:00Z"),
                EventOccurrence(startsAt = "2026-05-24T22:00:00Z", endsAt = "2026-05-25T01:00:00Z")
            ),
            priceOptions = listOf(
                EventPriceOption(
                    type = EventPriceType.Free,
                    condition = LocalizedText(default = "Children under 12")
                ),
                EventPriceOption(
                    type = EventPriceType.Paid,
                    amountMinorUnits = 1800,
                    currencyCode = "EUR",
                    condition = LocalizedText(default = "Standard admission")
                )
            )
        )
        renderFeed(FeedScreenTestDependencies(items = listOf(item)).viewModel) {}

        mediaBackground().performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("DateLabel").assertIsDisplayed()
        composeRule.onNodeWithText("Occurrence: $TestDate - 2026-05-23T02:00:00Z").assertIsDisplayed()
        composeRule.onNodeWithText("Occurrence: 2026-05-24T22:00:00Z - 2026-05-25T01:00:00Z").assertIsDisplayed()
        composeRule.onNodeWithText("PriceLabel").assertIsDisplayed()
        composeRule.onNodeWithText("Free - Children under 12").assertIsDisplayed()
        composeRule.onNodeWithText("€ 18 - Standard admission").assertIsDisplayed()
    }

    @Test
    fun legacyItem_withoutAttendanceOptions_showsContextDetails() {
        val item = feedScreenTestItem(attendanceOptions = emptyList(), contextDetails = "Legacy venue")
        renderFeed(FeedScreenTestDependencies(items = listOf(item)).viewModel) {}

        composeRule.onAllNodesWithText("Legacy venue", useUnmergedTree = true)[0].assertIsDisplayed()
    }

    @Test
    fun textLongPress_keepsTextSelection_withoutOpeningDetailsSheet() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel) {}

        composeRule.onAllNodesWithText(
            "Rooftop concert",
            useUnmergedTree = true
        )[0].performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("EventDetailsDescription").assertDoesNotExist()
    }

    @Test
    fun rightToLeftSwipe_fromMiddle_opensMenu() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel, layoutDirection = LayoutDirection.Ltr) {}

        swipeMenuRightToLeft()

        composeRule.onNodeWithText("FavoritesTitle").assertIsDisplayed()
    }

    @Test
    fun leftToRightSwipe_inRtl_opensMenu() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel, language = "ar", layoutDirection = LayoutDirection.Rtl) {}

        swipeMenuLeftToRight()

        composeRule.onNodeWithText("FavoritesTitle").assertIsDisplayed()
    }

    @Test
    fun rightToLeftSwipe_inRtl_doesNotOpenMenu() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel, language = "ar", layoutDirection = LayoutDirection.Rtl) {}

        swipeMenuRightToLeft()

        composeRule.onNodeWithText("FavoritesTitle").assertDoesNotExist()
    }

    @Test
    fun fastShortRightToLeftSwipe_opensMenu() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel) {}

        feedSurface().performTouchInput {
            swipeWithVelocity(
                start = Offset(width * 0.60f, height / 2f),
                end = Offset(width * 0.48f, height / 2f),
                endVelocity = 2_000f,
                durationMillis = 100L
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("FavoritesTitle").assertIsDisplayed()
    }

    @Test
    fun slowShortRightToLeftSwipe_doesNotOpenMenu() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel, layoutDirection = LayoutDirection.Ltr) {}

        feedSurface().performTouchInput {
            swipe(
                start = Offset(width * 0.60f, height / 2f),
                end = Offset(width * 0.56f, height / 2f),
                durationMillis = 2_000L
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("FavoritesTitle").assertDoesNotExist()
    }

    @Test
    fun verticalOrLeftToRightSwipe_doesNotOpenMenu() {
        val dependencies = FeedScreenTestDependencies()
        renderFeed(dependencies.viewModel) {}

        feedSurface().performTouchInput {
            swipe(
                start = Offset(width / 2f, height / 2f),
                end = Offset(width / 2f, height / 4f)
            )
            swipe(
                start = Offset(width * 0.30f, height / 2f),
                end = Offset(width * 0.70f, height / 2f)
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("FavoritesTitle").assertDoesNotExist()
    }

    @Test
    fun verticalSwipe_onMedia_stillPagesFeed_withoutOpeningMenu() {
        val dependencies = FeedScreenTestDependencies(
            items = listOf(
                feedScreenTestItem(),
                feedScreenTestItem(
                    id = "00000000-0000-0000-0000-000000000002",
                    sequence = 2,
                    title = "Gallery night"
                )
            )
        )
        renderFeed(dependencies.viewModel) {}

        mediaBackground().performTouchInput {
            swipe(
                start = Offset(width / 2f, height * 0.70f),
                end = Offset(width / 2f, height * 0.30f)
            )
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Gallery night", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onNodeWithText("FavoritesTitle").assertDoesNotExist()
    }

    @Test
    fun loadingMore_andNewItems_pills_stackBelowSearch_withoutOverlap() {
        val loadMoreGate = CompletableDeferred<Unit>()
        val items = listOf(
            feedScreenTestItem(),
            feedScreenTestItem(
                id = "00000000-0000-0000-0000-000000000002",
                sequence = 2,
                title = "Gallery night"
            ),
            feedScreenTestItem(
                id = "00000000-0000-0000-0000-000000000003",
                sequence = 3,
                title = "Street festival"
            )
        )
        val dependencies = FeedScreenTestDependencies(
            items = items,
            backgroundNewSinceCount = 2,
            loadMoreGate = loadMoreGate
        )
        renderFeed(dependencies.viewModel) {}
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag(FeedNewItemsPillTestTag).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.runOnIdle {
            dependencies.viewModel.updateCurrentIndex(items.lastIndex)
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag(FeedLoadingMorePillTestTag).fetchSemanticsNodes().isNotEmpty()
        }

        val searchBounds = composeRule.onNodeWithTag(FeedSearchPillTestTag).fetchSemanticsNode().boundsInRoot
        val loadingBounds = composeRule.onNodeWithTag(FeedLoadingMorePillTestTag).fetchSemanticsNode().boundsInRoot
        val newItemsBounds = composeRule.onNodeWithTag(FeedNewItemsPillTestTag).fetchSemanticsNode().boundsInRoot

        assertTrue(searchBounds.bottom <= loadingBounds.top)
        assertTrue(loadingBounds.bottom <= newItemsBounds.top)

        loadMoreGate.complete(Unit)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag(FeedLoadingMorePillTestTag).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag(FeedNewItemsPillTestTag).assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag(FeedNewItemsPillTestTag).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun assertFeedTeaserPrice(
        priceOptions: List<EventPriceOption>,
        expectedPrice: String,
        expectsAdditionalDetails: Boolean
    ) {
        renderFeed(
            FeedScreenTestDependencies(
                items = listOf(feedScreenTestItem(priceOptions = priceOptions))
            ).viewModel
        ) {}

        composeRule.onAllNodesWithText("Tue, Aug 19", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("·", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithText(expectedPrice, useUnmergedTree = true)[0].assertIsDisplayed()
        if (expectsAdditionalDetails) {
            composeRule.onAllNodesWithText("+", useUnmergedTree = true)[0].assertIsDisplayed()
        } else {
            composeRule.onAllNodesWithText("+", useUnmergedTree = true).assertCountEquals(0)
        }
    }

    private fun renderFeed(
        viewModel: FeedViewModel,
        language: String = "en",
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onOpenSettings: () -> Unit = {},
        onOpenFavorites: () -> Unit
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LocalEventsTheme(darkTheme = false) {
                    FeedScreen(
                        language = language,
                        mediaAutoplayEnabled = false,
                        darkTheme = false,
                        onOpenSettings = onOpenSettings,
                        onOpenFavorites = onOpenFavorites,
                        viewModel = viewModel
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onAllNodesWithContentDescription(
                    "AddFavoriteDescription",
                    useUnmergedTree = true
                )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }.getOrDefault(false)
        }
        favoriteButton().assertExists()
    }

    private fun favoriteButton() =
        composeRule.onAllNodesWithContentDescription(
            "AddFavoriteDescription",
            useUnmergedTree = true
        )[0]

    private fun menuButton() =
        composeRule.onNodeWithContentDescription(
            "MenuButtonDescription",
            useUnmergedTree = true
        )

    private fun mediaBackground() =
        composeRule.onAllNodesWithContentDescription(
            "Rooftop concert",
            useUnmergedTree = true
        )[0]

    private fun feedSurface() =
        composeRule.onRoot()

    private fun swipeMenuRightToLeft() {
        feedSurface().performTouchInput {
            swipe(
                start = Offset(width * 0.85f, height / 2f),
                end = Offset(width * 0.15f, height / 2f),
                durationMillis = 300L
            )
        }
        composeRule.waitForIdle()
    }

    private fun swipeMenuLeftToRight() {
        feedSurface().performTouchInput {
            swipe(
                start = Offset(width * 0.15f, height / 2f),
                end = Offset(width * 0.85f, height / 2f),
                durationMillis = 300L
            )
        }
        composeRule.waitForIdle()
    }
}

private class FeedScreenTestDependencies(
    val items: List<EventFeedItem> = listOf(feedScreenTestItem()),
    backgroundNewSinceCount: Int = 0,
    loadMoreGate: CompletableDeferred<Unit>? = null
) {
    val item = items.first()
    val favoritesStore = TrackingFavoritesStore()
    val telemetryRecorder = TrackingTelemetryRecorder()
    private val textProvider = TestTextProvider()
    val viewModel = FeedViewModel(
        feedApi = TestFeedRemoteDataSource(items, backgroundNewSinceCount, loadMoreGate),
        feedCacheRepository = ReadyFeedCacheStore(items),
        searchHistoryRepository = EmptySearchHistoryStore(),
        favoriteRepository = favoritesStore,
        telemetryRepository = telemetryRecorder,
        settingsRepository = FixedSettingsStore(),
        searchParser = FeedSearchParser(textProvider),
        localizer = textProvider,
        dateFormatter = FixedDateFormatter()
    )
}

private class TestFeedRemoteDataSource(
    private val items: List<EventFeedItem>,
    private val backgroundNewSinceCount: Int,
    private val loadMoreGate: CompletableDeferred<Unit>?
) : FeedRemoteDataSource {
    override suspend fun getFeed(
        language: String,
        query: FeedQuery,
        page: Int,
        pageSize: Int,
        knownCursor: String?
    ): FeedPageResponse {
        if (page > 0) {
            loadMoreGate?.await()
        }

        return FeedPageResponse(items = items, generatedAt = TestDate, newSinceCount = backgroundNewSinceCount)
    }

    override suspend fun searchAttendanceScopes(text: String, language: String, limit: Int): List<FeedAttendanceScope> =
        emptyList()

    override suspend fun searchCategories(text: String, language: String, limit: Int): List<FeedCategoryOption> =
        emptyList()
}

private class ReadyFeedCacheStore(private val items: List<EventFeedItem>) : FeedCacheStore {
    override suspend fun readLastSnapshot(): FeedSnapshot =
        FeedSnapshot(
            language = "en",
            query = FeedQuery(attendanceScopeId = TestLocation.id, attendanceScope = TestLocation),
            page = 0,
            pageSize = 10,
            response = FeedPageResponse(items = items, generatedAt = TestDate),
            cachedAt = TestDate
        )

    override suspend fun writeLastSnapshot(snapshot: FeedSnapshot) = Unit
}

private class EmptySearchHistoryStore : SearchHistoryStore {
    override suspend fun read(): List<FeedSearchHistoryEntry> = emptyList()

    override suspend fun save(entry: FeedSearchHistoryEntry) = Unit
}

private class TrackingFavoritesStore : FavoritesStore {
    private val favoritesState = MutableStateFlow<List<EventFeedItem>>(emptyList())
    val items: List<EventFeedItem>
        get() = favoritesState.value

    override val favorites: Flow<List<EventFeedItem>> = favoritesState

    override suspend fun current(): List<EventFeedItem> = favoritesState.value

    override suspend fun setFavorite(item: EventFeedItem, isFavorited: Boolean) {
        favoritesState.value = if (isFavorited) listOf(item.copy(isFavorited = true)) else emptyList()
    }

    override suspend fun refreshMetadata(items: List<EventFeedItem>) = Unit
}

private class TrackingTelemetryRecorder : TelemetryRecorder {
    val favoriteEvents = mutableListOf<Pair<String, Boolean>>()

    override fun start() = Unit

    override fun stop() = Unit

    override suspend fun recordView(eventId: String) = Unit

    override suspend fun recordFavorite(eventId: String, isFavorited: Boolean) {
        favoriteEvents += eventId to isFavorited
    }

    override suspend fun recordShareIntent(eventId: String) = Unit
}

private class FixedSettingsStore : SettingsStore {
    private val settingsState = MutableStateFlow(SettingsState(effectiveLanguage = "en"))

    override val settings: Flow<SettingsState> = settingsState

    override suspend fun current(): SettingsState = settingsState.value

    override suspend fun setLanguageTag(languageTag: String?) = Unit

    override suspend fun migrateLegacyLanguagePreference() = Unit

    override fun refreshLocaleState() = Unit

    override fun previewFloatingSurfaceOpacityLevel(level: Float) {}

    override suspend fun saveFloatingSurfaceOpacityLevel() {}

    override suspend fun setThemePreference(preference: AppThemePreference) = Unit

    override suspend fun setMediaAutoplayEnabled(enabled: Boolean) = Unit

    override suspend fun setHouseholdChildAges(ages: List<Int>) = Unit
}

private class TestTextProvider : AppTextProvider {
    override fun get(key: String, language: String): String =
        when (key) {
            "AttendanceOnline" -> "Online"
            "AttendanceTelevision" -> "TV"
            "FilterFree" -> "Free"
            else -> key
        }

    override fun format(key: String, language: String, vararg args: Any?): String =
        when (key) {
            "AttendanceModeDetailFormat" -> "${args[0]}: ${args[1]}"
            "AttendanceAdditionalOptionsFormat" -> "${args[0]}, ${args[1]} more attendance options"
            "AttendancePlaceAddressFormat" -> "${args[0]}, ${args[1]}"
            "PriceAdditionalDetailsFormat" -> "${args[0]}, additional pricing details available"
            else -> args.foldIndexed(key) { index, current, value -> current.replace("{$index}", value?.toString().orEmpty()) }
        }
}

private class FixedDateFormatter : DateTextFormatter {
    override fun formatEventStart(startDate: String, language: String): String = startDate

    override fun formatFeedDate(startDate: String, language: String): String = "Tue, Aug 19"

    override fun formatOccurrence(startsAt: String, endsAt: String?, language: String): String =
        "Occurrence: $startsAt${endsAt?.let { " - $it" }.orEmpty()}"

    override fun formatDateRange(from: String?, to: String?, language: String): String =
        listOfNotNull(from, to).joinToString("-")
}

private fun feedScreenTestItem(
    id: String = "00000000-0000-0000-0000-000000000001",
    sequence: Int = 1,
    title: String = "Rooftop concert",
    longDescription: String? = null,
    contextDetails: String = "Paris",
    attendanceOptions: List<EventAttendanceOption> = listOf(physicalAttendance("Paris", "paris")),
    occurrences: List<EventOccurrence> = listOf(EventOccurrence(startsAt = TestDate)),
    priceOptions: List<EventPriceOption> = listOf(EventPriceOption(type = EventPriceType.Free))
): EventFeedItem =
    EventFeedItem(
        id = id,
        sequence = sequence,
        media = Media(type = MediaType.Image, url = "https://example.com/image.jpg"),
        category = LocalizedText(default = "Music"),
        title = LocalizedText(default = title),
        description = LocalizedText(default = "Live set"),
        longDescription = longDescription?.let { LocalizedText(default = it) },
        contextDetails = contextDetails,
        locationScopeId = TestLocation.id,
        attendanceOptions = attendanceOptions,
        startDate = TestDate,
        occurrences = occurrences,
        publishedAt = TestDate,
        updatedAt = TestDate,
        priceLabel = "Free",
        priceOptions = priceOptions,
        source = EventSource(name = "Local Events", type = "Official")
    )

private fun physicalAttendance(name: String, locationScopeId: String): EventAttendanceOption =
    EventAttendanceOption(
        type = EventAttendanceType.Physical,
        displayName = LocalizedText(default = name),
        locationScopeId = locationScopeId,
        isPrimary = true
    )

private val TestLocation = FeedAttendanceScope(
    id = "paris",
    name = "Paris",
    displayPath = "Paris, France",
    isSearchable = true
)

private const val TestDate = "2026-05-23T00:00:00Z"
