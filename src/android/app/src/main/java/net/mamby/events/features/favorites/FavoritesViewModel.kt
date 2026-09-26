package net.mamby.events.features.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.mamby.events.core.AppTextProvider
import net.mamby.events.core.AttendanceSummaryText
import net.mamby.events.core.DateTextFormatter
import net.mamby.events.core.EventFeedItem
import net.mamby.events.core.FavoritesStore
import net.mamby.events.core.SettingsStore
import net.mamby.events.core.SettingsState
import net.mamby.events.core.attendanceSummaryText

data class FavoritesUiState(
    val settings: SettingsState = SettingsState(),
    val items: List<EventFeedItem> = emptyList()
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val localizer: AppTextProvider,
    private val dateFormatter: DateTextFormatter,
    settingsRepository: SettingsStore,
    favoritesRepository: FavoritesStore
) : ViewModel() {
    val state = combine(settingsRepository.settings, favoritesRepository.favorites) { settings, favorites ->
        FavoritesUiState(settings = settings, items = favorites)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoritesUiState()
    )

    fun string(key: String): String =
        localizer.get(key, state.value.settings.effectiveLanguage)

    fun categoryText(item: EventFeedItem): String =
        item.category.resolve(state.value.settings.effectiveLanguage)

    fun titleText(item: EventFeedItem): String =
        item.title.resolve(state.value.settings.effectiveLanguage)

    fun scheduleAndPrice(item: EventFeedItem): String =
        "${dateFormatter.formatEventStart(item.startDate, state.value.settings.effectiveLanguage)} ${item.priceLabel}".trim()

    fun attendanceSummary(item: EventFeedItem): AttendanceSummaryText =
        attendanceSummaryText(item, null, state.value.settings.effectiveLanguage, localizer)

    fun ageRestrictionText(item: EventFeedItem): String =
        when (val minimumAge = item.ageRestriction?.minimumRequiredAge) {
            null -> if (item.ageRestriction == null) {
                string("AgeRestrictionUnknown")
            } else {
                string("AgeRestrictionPublic")
            }
            else -> localizer.format("AgeRestrictionMinimumFormat", state.value.settings.effectiveLanguage, minimumAge)
        }

    fun recommendedAgeText(item: EventFeedItem): String =
        item.recommendedAge?.let { recommended ->
            recommended.maximumAge?.let { maximumAge ->
                localizer.format(
                    "RecommendedAgeRangeFormat",
                    state.value.settings.effectiveLanguage,
                    recommended.minimumAge,
                    maximumAge
                )
            } ?: localizer.format(
                "RecommendedAgeMinimumFormat",
                state.value.settings.effectiveLanguage,
                recommended.minimumAge
            )
        }.orEmpty()
}
