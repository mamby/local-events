package net.mamby.events.core

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.PluralsRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.Normalizer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import net.mamby.events.R

@Singleton
class AppLocalizer @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AppTextProvider {
    override fun get(key: String, language: String): String =
        stringResourceIds[key]
            ?.let { localizedContext(language).getString(it) }
            ?: key

    override fun format(key: String, language: String, vararg args: Any?): String {
        pluralResourceIds[key]?.let { plural ->
            val quantity = args.getOrNull(plural.quantityArgumentIndex) as? Int
            if (quantity != null) {
                return localizedContext(language).resources.getQuantityString(
                    plural.id,
                    quantity,
                    *args
                )
            }
        }

        return stringResourceIds[key]
            ?.let { localizedContext(language).getString(it, *args) }
            ?: key
    }

    private fun localizedContext(language: String): Context {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(normalizeAppLanguageTag(language)))
        }
        return context.createConfigurationContext(configuration)
    }

    data class PluralResource(
        @param:PluralsRes val id: Int,
        val quantityArgumentIndex: Int
    )

    companion object {
        internal val pluralResourceIds = mapOf(
            "AttendanceAdditionalOptionsFormat" to PluralResource(R.plurals.attendance_additional_options_format, quantityArgumentIndex = 1),
            "FiltersCountFormat" to PluralResource(R.plurals.filters_count_format, quantityArgumentIndex = 0),
            "NewResultsFormat" to PluralResource(R.plurals.new_results_format, quantityArgumentIndex = 0)
        )

        internal val stringResourceIds = mapOf(
            "AboutLabel" to R.string.about_label,
            "AccessibleForAgeFilterFormat" to R.string.accessible_for_age_filter_format,
            "AccessibilityFilterFormat" to R.string.accessibility_filter_format,
            "AccessibilityLabel" to R.string.accessibility_label,
            "AccessibilitySearchPlaceholder" to R.string.accessibility_search_placeholder,
            "AddCustomCriterionFormat" to R.string.add_custom_criterion_format,
            "AddFavoriteDescription" to R.string.add_favorite_description,
            "AddFavoriteHint" to R.string.add_favorite_hint,
            "AddLocation" to R.string.add_location,
            "AddLocationHint" to R.string.add_location_hint,
            "AddSearchTerm" to R.string.add_search_term,
            "AddSearchTermHint" to R.string.add_search_term_hint,
            "AgeInformationLabel" to R.string.age_information_label,
            "AgeInputError" to R.string.age_input_error,
            "AgeRestrictionLabel" to R.string.age_restriction_label,
            "AgeRestrictionMinimumFormat" to R.string.age_restriction_minimum_format,
            "AgeRestrictionPublic" to R.string.age_restriction_public,
            "AgeRestrictionUnknown" to R.string.age_restriction_unknown,
            "AgeYearsPlaceholder" to R.string.age_years_placeholder,
            "AppliedLabel" to R.string.applied_label,
            "AttendanceInputError" to R.string.attendance_input_error,
            "AttendanceLabel" to R.string.attendance_label,
            "AttendanceModeDetailFormat" to R.string.attendance_mode_detail_format,
            "AttendanceOnline" to R.string.attendance_online,
            "AttendanceOptionsLabel" to R.string.attendance_options_label,
            "AttendancePlaceAddressFormat" to R.string.attendance_place_address_format,
            "AttendancePlaceholder" to R.string.attendance_placeholder,
            "AttendanceRequiredError" to R.string.attendance_required_error,
            "AttendanceRequiredLabel" to R.string.attendance_required_label,
            "AttendanceTelevision" to R.string.attendance_television,
            "AppName" to R.string.app_name,
            "Apply" to R.string.apply,
            "AnyCategory" to R.string.any_category,
            "AnyDate" to R.string.any_date,
            "AnyAgeRestriction" to R.string.any_age_restriction,
            "AnyPrice" to R.string.any_price,
            "AutoplayLabel" to R.string.autoplay_label,
            "BackDescription" to R.string.back_description,
            "BackHint" to R.string.back_hint,
            "CategoryCinema" to R.string.category_cinema,
            "CategoryFood" to R.string.category_food,
            "CategoryLabel" to R.string.category_label,
            "CategorySearchUnavailableError" to R.string.category_search_unavailable_error,
            "CategoryMusic" to R.string.category_music,
            "CategorySports" to R.string.category_sports,
            "CategoryTech" to R.string.category_tech,
            "CategoryWellness" to R.string.category_wellness,
            "ChooseLocation" to R.string.choose_location,
            "ChooseAttendance" to R.string.choose_attendance,
            "ChooseDates" to R.string.choose_dates,
            "ChooseDatesTitle" to R.string.choose_dates_title,
            "Clear" to R.string.clear,
            "ClearAllRecentSearches" to R.string.clear_all_recent_searches,
            "Close" to R.string.close,
            "Cancel" to R.string.cancel,
            "ContactHint" to R.string.contact_hint,
            "ContactTitle" to R.string.contact_title,
            "DateFromFormat" to R.string.date_from_format,
            "DateLabel" to R.string.date_label,
            "DateUntilFormat" to R.string.date_until_format,
            "Done" to R.string.done,
            "EditFilters" to R.string.edit_filters,
            "EventDetailsDescription" to R.string.event_details_description,
            "EventDetailsHint" to R.string.event_details_hint,
            "FavoritesCardHint" to R.string.favorites_card_hint,
            "FavoritesTitle" to R.string.favorites_title,
            "FeedbackHint" to R.string.feedback_hint,
            "FeedbackTitle" to R.string.feedback_title,
            "FeedLoadErrorFallback" to R.string.feed_load_error_fallback,
            "FeedLoadErrorMessage" to R.string.feed_load_error_message,
            "FeedUnavailableTitle" to R.string.feed_unavailable_title,
            "FilterFree" to R.string.filter_free,
            "FilterPaid" to R.string.filter_paid,
            "FilterThisWeekend" to R.string.filter_this_weekend,
            "FilterToday" to R.string.filter_today,
            "FilterTomorrow" to R.string.filter_tomorrow,
            "FiltersLabel" to R.string.filters_label,
            "FindEvents" to R.string.find_events,
            "LanguageArabic" to R.string.language_arabic,
            "LanguageEnglish" to R.string.language_english,
            "LanguageFrench" to R.string.language_french,
            "LanguageLabel" to R.string.language_label,
            "LanguageSystemDefault" to R.string.language_system_default,
            "LoadingLocalEvents" to R.string.loading_local_events,
            "LoadingCategories" to R.string.loading_categories,
            "LoadingMore" to R.string.loading_more,
            "LoadingResults" to R.string.loading_results,
            "LoadingSuggestions" to R.string.loading_suggestions,
            "LocationInputError" to R.string.location_input_error,
            "MediaLabel" to R.string.media_label,
            "LocationLabel" to R.string.location_label,
            "LocationPermissionUsageDescription" to R.string.location_permission_usage_description,
            "LocationPlaceholder" to R.string.location_placeholder,
            "LocationRequiredError" to R.string.location_required_error,
            "LocationRequiredLabel" to R.string.location_required_label,
            "LocationsLabel" to R.string.locations_label,
            "MyHouseholdFilterFormat" to R.string.my_household_filter_format,
            "MenuButtonDescription" to R.string.menu_button_description,
            "MenuTitle" to R.string.menu_title,
            "NearFormat" to R.string.near_format,
            "NewResultOne" to R.string.new_result_one,
            "NoFavoritesSubtitle" to R.string.no_favorites_subtitle,
            "NoFavoritesTitle" to R.string.no_favorites_title,
            "NoLocalEventsSubtitle" to R.string.no_local_events_subtitle,
            "NoLocalEventsTitle" to R.string.no_local_events_title,
            "NoLanguageResults" to R.string.no_language_results,
            "NoCategoriesFound" to R.string.no_categories_found,
            "NoLocationsFound" to R.string.no_locations_found,
            "NoRecentSearches" to R.string.no_recent_searches,
            "NoSearchTermsFound" to R.string.no_search_terms_found,
            "OpenSourceLicensesHint" to R.string.open_source_licenses_hint,
            "OpenSourceLicensesTitle" to R.string.open_source_licenses_title,
            "OpenSourceHint" to R.string.open_source_hint,
            "OpenSourceTitle" to R.string.open_source_title,
            "OrganizerHint" to R.string.organizer_hint,
            "OrganizerTitle" to R.string.organizer_title,
            "ApplyFilterHint" to R.string.apply_filter_hint,
            "PausePlaybackDescription" to R.string.pause_playback_description,
            "PlayPlaybackDescription" to R.string.play_playback_description,
            "PlaybackToggleHint" to R.string.playback_toggle_hint,
            "PlaybackUnavailableDescription" to R.string.playback_unavailable_description,
            "PlaybackUnavailableHint" to R.string.playback_unavailable_hint,
            "PrivacyHint" to R.string.privacy_hint,
            "PrivacyTitle" to R.string.privacy_title,
            "PriceLabel" to R.string.price_label,
            "PriceAdditionalDetailsFormat" to R.string.price_additional_details_format,
            "RecentLabel" to R.string.recent_label,
            "RecentSearchesTitle" to R.string.recent_searches_title,
            "Refresh" to R.string.refresh,
            "ReloadNewResultsHint" to R.string.reload_new_results_hint,
            "RemoveFavoriteDescription" to R.string.remove_favorite_description,
            "RemoveFavoriteHint" to R.string.remove_favorite_hint,
            "RemoveLocationFormat" to R.string.remove_location_format,
            "RemoveRecentSearchFormat" to R.string.remove_recent_search_format,
            "RemoveSearchTermFormat" to R.string.remove_search_term_format,
            "RestoreSearchDraftHint" to R.string.restore_search_draft_hint,
            "Retry" to R.string.retry,
            "ResetFilters" to R.string.reset_filters,
            "RemoveFilterHint" to R.string.remove_filter_hint,
            "RecommendedAgeMinimumFormat" to R.string.recommended_age_minimum_format,
            "RecommendedAgeRangeFormat" to R.string.recommended_age_range_format,
            "SearchEventsPlaceholder" to R.string.search_events_placeholder,
            "SearchCategoriesPlaceholder" to R.string.search_categories_placeholder,
            "SearchLanguagesPlaceholder" to R.string.search_languages_placeholder,
            "SearchLocationsPlaceholder" to R.string.search_locations_placeholder,
            "SearchLocationsTitle" to R.string.search_locations_title,
            "SearchNoResultsFeedback" to R.string.search_no_results_feedback,
            "SearchPillHint" to R.string.search_pill_hint,
            "SearchUnavailableError" to R.string.search_unavailable_error,
            "SearchSuggestionsUnavailable" to R.string.search_suggestions_unavailable,
            "SearchTermsLabel" to R.string.search_terms_label,
            "SearchTermsPlaceholder" to R.string.search_terms_placeholder,
            "SearchTermsTitle" to R.string.search_terms_title,
            "SeeAllRecentSearchesFormat" to R.string.see_all_recent_searches_format,
            "ShowResults" to R.string.show_results,
            "SelectLocationHint" to R.string.select_location_hint,
            "SettingsButtonHint" to R.string.settings_button_hint,
            "SettingsTitle" to R.string.settings_title,
            "ShareCurrentEventHint" to R.string.share_current_event_hint,
            "ShareEventDescription" to R.string.share_event_description,
            "ShareSubtitle" to R.string.share_subtitle,
            "ShareThisEventHint" to R.string.share_this_event_hint,
            "ShareTitle" to R.string.share_title,
            "SuggestedLabel" to R.string.suggested_label,
            "ThemeDark" to R.string.theme_dark,
            "ThemeLabel" to R.string.theme_label,
            "ThemeLight" to R.string.theme_light,
            "ThemeSystemDefault" to R.string.theme_system_default,
            "TermsHint" to R.string.terms_hint,
            "TermsTitle" to R.string.terms_title,
            "VersionLabel" to R.string.version_label,
            "YoungestParticipantAgeLabel" to R.string.youngest_participant_age_label
        )
    }
}

interface AppTextProvider {
    fun get(key: String, language: String): String

    fun format(key: String, language: String, vararg args: Any?): String
}

@Singleton
class AppDateFormatter @Inject constructor(
    private val localizer: AppTextProvider
) : DateTextFormatter {
    override fun formatEventStart(startDate: String, language: String): String {
        val date = parseOffsetDateTime(startDate) ?: return startDate
        val formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(localeFor(language))
        return date.format(formatter)
    }

    override fun formatFeedDate(startDate: String, language: String): String {
        val date = parseOffsetDateTime(startDate) ?: return startDate
        val formatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(localeFor(language))
        return date.format(formatter)
    }

    override fun formatOccurrence(startsAt: String, endsAt: String?, language: String): String {
        val start = parseOffsetDateTime(startsAt) ?: return startsAt
        val end = endsAt?.let(::parseOffsetDateTime)
        val locale = localeFor(language)
        val dateTimeFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
        val timeFormatter = DateTimeFormatter
            .ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
        val formattedStart = start.format(dateTimeFormatter)

        return when {
            end == null -> formattedStart
            end.toLocalDate() == start.toLocalDate() ->
                "$formattedStart - ${end.format(timeFormatter)}"
            else -> "$formattedStart - ${end.format(dateTimeFormatter)}"
        }
    }

    override fun formatDateRange(from: String?, to: String?, language: String): String =
        when {
            !from.isNullOrBlank() && from == to -> formatShortDate(from, language)
            !from.isNullOrBlank() && !to.isNullOrBlank() -> "${formatShortDate(from, language)}-${formatShortDate(to, language)}"
            !from.isNullOrBlank() -> localizer.format("DateFromFormat", language, formatShortDate(from, language))
            !to.isNullOrBlank() -> localizer.format("DateUntilFormat", language, formatShortDate(to, language))
            else -> ""
        }

    private fun formatShortDate(dateText: String, language: String): String {
        val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return dateText
        val formatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(localeFor(language))
        return date.format(formatter)
    }

    private fun localeFor(language: String): Locale =
        Locale.forLanguageTag(normalizeAppLanguageTag(language))

    private fun parseOffsetDateTime(value: String): OffsetDateTime? =
        runCatching { OffsetDateTime.parse(value) }.getOrNull()
}

interface DateTextFormatter {
    fun formatEventStart(startDate: String, language: String): String

    fun formatFeedDate(startDate: String, language: String): String

    fun formatOccurrence(startsAt: String, endsAt: String?, language: String): String

    fun formatDateRange(from: String?, to: String?, language: String): String
}

@Singleton
class FeedSearchParser @Inject constructor(
    private val localizer: AppTextProvider
) {
    private val categoryRules = listOf(
        CategoryRule("CategoryMusic", "Music", Regex("""(?:\b(?:music|musique|música|musica|musik|muziek|muzyka|âm\s+nhạc|am\s+nhac|concerts?|live\s+set|dj)\b|موسيقى|حفلات?|حفل|دي\s*جي|音乐|音樂|संगीत|音楽|음악|müzik|музыка|ดนตรี)""", RegexOption.IGNORE_CASE)),
        CategoryRule("CategorySports", "Sports", Regex("""(?:\b(?:sport|sports|deportes|esportes|olahraga|sporty|thể\s+thao|the\s+thao|run|running|match|fitness)\b|رياضة|رياضي|مباراة|جري|لياقة|体育|體育|खेल|スポーツ|스포츠|spor|спорт|กีฬา)""", RegexOption.IGNORE_CASE)),
        CategoryRule("CategoryFood", "Food", Regex("""(?:\b(?:food|comida|cibo|essen|eten|jedzenie|makanan|ẩm\s+thực|am\s+thuc|cuisine|street\s+food|market|tasting|ramen|bao)\b|طعام|أكل|اكل|مأكولات|سوق|تذوق|美食|อาหาร|भोजन|フード|食べ物|음식|yemek|еда)""", RegexOption.IGNORE_CASE)),
        CategoryRule("CategoryTech", "Tech", Regex("""(?:\b(?:tech|tecnología|tecnologia|technik|technologie|teknologi|tecnologia|teknoloji|technologia|công\s+nghệ|cong\s+nghe|startup|founders?|networking|demo)\b|تقنية|تكنولوجيا|تقني|شركات\s+ناشئة|ستارت\s*اب|科技|टेक|技術|テック|기술|технологии|เทคโนโลยี)""", RegexOption.IGNORE_CASE)),
        CategoryRule("CategoryWellness", "Wellness", Regex("""(?:\b(?:wellness|bien-etre|bienestar|bem-estar|benessere|wellness|welzijn|zdrowie|kebugaran|sức\s+khỏe|suc\s+khoe|yoga|meditation)\b|عافية|صحة|يوغا|تأمل|健康|वेलनेस|ウェルネス|웰니스|sağlık|saglik|здоровье|สุขภาพ)""", RegexOption.IGNORE_CASE)),
        CategoryRule("CategoryCinema", "Cinema", Regex("""(?:\b(?:cinema|cine|kino|bioscoop|film|films?|película|pelicula|filme|sinema|кино|điện\s+ảnh|dien\s+anh)\b|سينما|فيلم|أفلام|افلام|电影|電影|सिनेमा|映画|영화|ภาพยนตร์)""", RegexOption.IGNORE_CASE))
    )

    fun parse(text: String?, language: String, now: LocalDate = LocalDate.now()): FeedSearchParseResult {
        if (text.isNullOrBlank()) {
            return FeedSearchParseResult(null, emptyList())
        }

        var remaining = text.trim()
        val suggestions = mutableListOf<FeedFilterChip>()

        remaining = addPriceSuggestions(remaining, language, suggestions)
        remaining = addDateSuggestions(remaining, language, now, suggestions)
        remaining = addCategorySuggestions(remaining, language, suggestions)

        return FeedSearchParseResult(cleanSearchText(remaining), suggestions)
    }

    private fun addPriceSuggestions(
        text: String,
        language: String,
        suggestions: MutableList<FeedFilterChip>
    ): String {
        var remaining = text
        if (freePriceRegex.containsMatchIn(remaining)) {
            suggestions += FeedFilterChip(
                kind = FeedFilterKind.Price,
                label = localizer.get("FilterFree", language),
                query = FeedQuery(priceFilter = FeedPriceFilter.Free)
            )
            remaining = freePriceRegex.replace(remaining, " ")
        }

        if (paidPriceRegex.containsMatchIn(remaining)) {
            suggestions += FeedFilterChip(
                kind = FeedFilterKind.Price,
                label = localizer.get("FilterPaid", language),
                query = FeedQuery(priceFilter = FeedPriceFilter.Paid)
            )
            remaining = paidPriceRegex.replace(remaining, " ")
        }

        return remaining
    }

    private fun addDateSuggestions(
        text: String,
        language: String,
        today: LocalDate,
        suggestions: MutableList<FeedFilterChip>
    ): String {
        var remaining = text
        if (weekendRegex.containsMatchIn(remaining)) {
            val (from, to) = weekendRange(today)
            suggestions += FeedFilterChip(
                kind = FeedFilterKind.DateRange,
                label = localizer.get("FilterThisWeekend", language),
                query = FeedQuery(dateFrom = from.toString(), dateTo = to.toString())
            )
            remaining = weekendRegex.replace(remaining, " ")
        }

        if (tomorrowRegex.containsMatchIn(remaining)) {
            val tomorrow = today.plusDays(1)
            suggestions += FeedFilterChip(
                kind = FeedFilterKind.DateRange,
                label = localizer.get("FilterTomorrow", language),
                query = FeedQuery(dateFrom = tomorrow.toString(), dateTo = tomorrow.toString())
            )
            remaining = tomorrowRegex.replace(remaining, " ")
        }

        if (todayRegex.containsMatchIn(remaining)) {
            suggestions += FeedFilterChip(
                kind = FeedFilterKind.DateRange,
                label = localizer.get("FilterToday", language),
                query = FeedQuery(dateFrom = today.toString(), dateTo = today.toString())
            )
            remaining = todayRegex.replace(remaining, " ")
        }

        return remaining
    }

    private fun addCategorySuggestions(
        text: String,
        language: String,
        suggestions: MutableList<FeedFilterChip>
    ): String {
        var remaining = text
        for (rule in categoryRules) {
            if (!rule.pattern.containsMatchIn(remaining)) {
                continue
            }

            suggestions += FeedFilterChip(
                kind = FeedFilterKind.Category,
                label = localizer.get(rule.labelResourceKey, language),
                query = FeedQuery(category = rule.queryValue)
            )
            remaining = rule.pattern.replace(remaining, " ")
        }

        return remaining
    }

    private fun weekendRange(today: LocalDate): Pair<LocalDate, LocalDate> =
        when (today.dayOfWeek.value) {
            6 -> today to today.plusDays(1)
            7 -> today.minusDays(1) to today
            else -> {
                val daysUntilSaturday = (6 - today.dayOfWeek.value + 7) % 7
                val saturday = today.plusDays(daysUntilSaturday.toLong())
                saturday to saturday.plusDays(1)
            }
        }

    private fun cleanSearchText(text: String): String? =
        whitespaceRegex.replace(text, " ")
            .trim(' ', ',', '.', ';', ':', '-')
            .takeIf(String::isNotBlank)

    private data class CategoryRule(
        val labelResourceKey: String,
        val queryValue: String,
        val pattern: Regex
    )

    private companion object {
        val freePriceRegex = Regex("""(?:\b(?:free|gratuit|gratis|kostenlos|grátis|gratuito|bezpłatne|bezplatne|miễn\s+phí|mien\s+phi|ücretsiz|ucretsiz|бесплатно|0\s*(?:euro|eur|usd|dollar|${'$'}))\b|مجاني|مجانا|بدون\s+رسوم|免费|免費|मुफ़्त|मुफ्त|無料|무료|ฟรี)""", RegexOption.IGNORE_CASE)
        val paidPriceRegex = Regex("""(?:\b(?:paid|payant|pago|kostenpflichtig|berbayar|a\s+pagamento|pagamento|ücretli|ucretli|платно|betaald|płatne|platne|trả\s+phí|tra\s+phi|ticketed|tickets?)\b|مدفوع|تذاكر|تذكرة|付费|付費|सशुल्क|有料|유료|มีค่าใช้จ่าย)""", RegexOption.IGNORE_CASE)
        val weekendRegex = Regex("""(?:\b(?:this\s+weekend|weekend|week-end|fin\s+de\s+semana|fim\s+de\s+semana|wochenende|akhir\s+pekan|hafta\s+sonu|выходные|cuối\s+tuần|cuoi\s+tuan)\b|نهاية\s+الأسبوع|نهاية\s+الاسبوع|الويكند|周末|सप्ताहांत|週末|주말|สุดสัปดาห์)""", RegexOption.IGNORE_CASE)
        val tomorrowRegex = Regex("""(?:\b(?:tomorrow|demain|mañana|amanhã|amanha|morgen|besok|domani|yarın|yarin|завтра|jutro|ngày\s+mai|ngay\s+mai)\b|غدا|غدًا|明天|कल|明日|내일|พรุ่งนี้)""", RegexOption.IGNORE_CASE)
        val todayRegex = Regex("""(?:\b(?:today|tonight|aujourd'hui|ce\s+soir|hoy|hoje|heute|hari\s+ini|oggi|bugün|bugun|сегодня|vandaag|dzisiaj|hôm\s+nay|hom\s+nay)\b|اليوم|الليلة|هذا\s+المساء|今天|आज|今日|오늘|วันนี้)""", RegexOption.IGNORE_CASE)
        val whitespaceRegex = Regex("""\s+""")
    }
}

fun FeedFilterChip.localized(
    query: FeedQuery,
    language: String,
    localizer: AppTextProvider,
    dateFormatter: DateTextFormatter
): FeedFilterChip {
    val label = when {
        kind == FeedFilterKind.Category && !query.category.isNullOrBlank() -> categoryDisplayText(query.category, language, localizer)
        kind == FeedFilterKind.DateRange && (!query.dateFrom.isNullOrBlank() || !query.dateTo.isNullOrBlank()) -> {
            dateFormatter.formatDateRange(query.dateFrom, query.dateTo, language)
        }
        kind == FeedFilterKind.Price && query.priceFilter != FeedPriceFilter.Any -> priceDisplayText(query.priceFilter, language, localizer)
        kind == FeedFilterKind.Accessibility && !query.accessibility.isNullOrBlank() ->
            accessibilityFilterDisplayText(query.accessibility, language, localizer)
        kind == FeedFilterKind.AgeRestriction && query.accessibleForAge != null ->
            accessibleForAgeDisplayText(query.accessibleForAge, language, localizer)
        else -> label
    }

    return copy(label = label)
}

fun categoryDisplayText(category: String, language: String, localizer: AppTextProvider): String =
    when (category) {
        "Music" -> localizer.get("CategoryMusic", language)
        "Sports" -> localizer.get("CategorySports", language)
        "Food" -> localizer.get("CategoryFood", language)
        "Tech" -> localizer.get("CategoryTech", language)
        "Wellness" -> localizer.get("CategoryWellness", language)
        "Cinema" -> localizer.get("CategoryCinema", language)
        else -> category
    }

fun priceDisplayText(priceFilter: FeedPriceFilter, language: String, localizer: AppTextProvider): String =
    when (priceFilter) {
        FeedPriceFilter.Free -> localizer.get("FilterFree", language)
        FeedPriceFilter.Paid -> localizer.get("FilterPaid", language)
        FeedPriceFilter.Any -> ""
    }

fun accessibilityFilterDisplayText(accessibility: String, language: String, localizer: AppTextProvider): String =
    localizer.format("AccessibilityFilterFormat", language, accessibility)

fun accessibleForAgeDisplayText(age: Int, language: String, localizer: AppTextProvider): String =
    localizer.format("AccessibleForAgeFilterFormat", language, age)

data class AttendanceSummaryText(
    val primaryText: String,
    val additionalCount: Int,
    val contentDescription: String
)

data class PriceSummaryText(
    val primaryText: String,
    val hasAdditionalDetails: Boolean,
    val contentDescription: String
)

fun priceSummaryText(
    item: EventFeedItem,
    language: String,
    localizer: AppTextProvider
): PriceSummaryText {
    if (item.priceOptions.isEmpty()) {
        return PriceSummaryText(
            primaryText = item.priceLabel,
            hasAdditionalDetails = false,
            contentDescription = item.priceLabel
        )
    }

    val freeOption = item.priceOptions.firstOrNull { it.type == EventPriceType.Free }
    val paidOptions = item.priceOptions.filter { it.type == EventPriceType.Paid }
    val primaryText = freeOption?.let { localizer.get("FilterFree", language) }
        ?: paidOptions.minBy { it.amountMinorUnits ?: Long.MAX_VALUE }.formatPrice(language)
    val hasAdditionalDetails = if (freeOption != null) {
        freeOption.condition != null || item.priceOptions.size > 1
    } else {
        paidOptions.size > 1 || paidOptions.any { it.condition != null }
    }
    val contentDescription = if (hasAdditionalDetails) {
        localizer.format("PriceAdditionalDetailsFormat", language, primaryText)
    } else {
        primaryText
    }

    return PriceSummaryText(primaryText, hasAdditionalDetails, contentDescription)
}

fun occurrenceDetailLines(
    item: EventFeedItem,
    language: String,
    dateFormatter: DateTextFormatter
): List<String> =
    (item.occurrences.ifEmpty { listOf(EventOccurrence(startsAt = item.startDate)) })
        .sortedBy { occurrence ->
            runCatching { OffsetDateTime.parse(occurrence.startsAt).toInstant() }.getOrNull()
        }
        .map { occurrence ->
            dateFormatter.formatOccurrence(occurrence.startsAt, occurrence.endsAt, language)
        }

fun priceDetailLines(
    item: EventFeedItem,
    language: String,
    localizer: AppTextProvider
): List<String> =
    item.priceOptions.ifEmpty { return listOf(item.priceLabel) }.map { option ->
        val price = when (option.type) {
            EventPriceType.Free -> localizer.get("FilterFree", language)
            EventPriceType.Paid -> option.formatPrice(language)
        }
        option.condition?.resolve(language)?.takeIf(String::isNotBlank)?.let { "$price - $it" } ?: price
    }

fun attendanceSummaryText(
    item: EventFeedItem,
    selectedScopeId: String?,
    language: String,
    localizer: AppTextProvider
): AttendanceSummaryText =
    attendanceSummaryText(
        item = item,
        selectedScopeIds = listOfNotNull(selectedScopeId),
        customLocations = emptyList(),
        language = language,
        localizer = localizer
    )

fun attendanceSummaryText(
    item: EventFeedItem,
    selectedScopeIds: List<String>,
    customLocations: List<String>,
    language: String,
    localizer: AppTextProvider
): AttendanceSummaryText {
    if (item.attendanceOptions.isEmpty()) {
        return AttendanceSummaryText(
            primaryText = item.contextDetails,
            additionalCount = 0,
            contentDescription = item.contextDetails
        )
    }

    val normalizedScopeIds = selectedScopeIds.map(FeedAttendanceScope::normalizeId).filter(String::isNotBlank)
    val primary = normalizedScopeIds.firstNotNullOfOrNull { scopeId ->
        when (scopeId) {
            FeedAttendanceScope.OnlineId ->
                item.attendanceOptions.firstOrNull { it.type == EventAttendanceType.Online }
            FeedAttendanceScope.TelevisionId ->
                item.attendanceOptions.firstOrNull { it.type == EventAttendanceType.Television }
            FeedAttendanceScope.WorldId ->
                item.attendanceOptions.firstOrNull(EventAttendanceOption::isPrimary)
            else -> item.attendanceOptions.firstOrNull {
                it.type == EventAttendanceType.Physical &&
                    FeedAttendanceScope.normalizeId(it.locationScopeId) == scopeId
            }
        }
    } ?: customLocations.firstNotNullOfOrNull { location ->
        item.attendanceOptions.firstOrNull { option ->
            option.type == EventAttendanceType.Physical &&
                listOfNotNull(option.displayName, option.address).any { text ->
                    listOf(text.resolve(language), text.default)
                        .plus(text.translations.values)
                        .any { it.contains(location, ignoreCase = true) }
                }
        }
    } ?: item.attendanceOptions.firstOrNull(EventAttendanceOption::isPrimary)
        ?: item.attendanceOptions.firstOrNull { it.type == EventAttendanceType.Physical }
        ?: item.attendanceOptions.firstOrNull { it.type == EventAttendanceType.Online }
        ?: item.attendanceOptions.first()

    val primaryText = attendanceOptionSummaryLabel(primary, language, localizer)
    val additionalCount = (item.attendanceOptions.size - 1).coerceAtLeast(0)
    val contentDescription = if (additionalCount == 0) {
        primaryText
    } else {
        localizer.format("AttendanceAdditionalOptionsFormat", language, primaryText, additionalCount)
    }

    return AttendanceSummaryText(primaryText, additionalCount, contentDescription)
}

fun attendanceDetailLines(
    item: EventFeedItem,
    language: String,
    localizer: AppTextProvider
): List<String> =
    if (item.attendanceOptions.isEmpty()) {
        listOfNotNull(item.contextDetails.takeIf(String::isNotBlank))
    } else {
        item.attendanceOptions.map { option ->
            when (option.type) {
                EventAttendanceType.Physical -> {
                    val name = option.displayName?.resolve(language).orEmpty()
                    val address = option.address?.resolve(language).orEmpty()
                    when {
                        name.isNotBlank() && address.isNotBlank() ->
                            localizer.format("AttendancePlaceAddressFormat", language, name, address)
                        name.isNotBlank() -> name
                        else -> address
                    }
                }
                EventAttendanceType.Online -> option.displayName?.resolve(language)
                    ?.takeIf(String::isNotBlank)
                    ?.let { localizer.format("AttendanceModeDetailFormat", language, localizer.get("AttendanceOnline", language), it) }
                    ?: localizer.get("AttendanceOnline", language)
                EventAttendanceType.Television -> option.displayName?.resolve(language)
                    ?.takeIf(String::isNotBlank)
                    ?.let { localizer.format("AttendanceModeDetailFormat", language, localizer.get("AttendanceTelevision", language), it) }
                    ?: localizer.get("AttendanceTelevision", language)
            }
        }.filter(String::isNotBlank)
    }

private fun EventPriceOption.formatPrice(language: String): String {
    val amount = amountMinorUnits ?: return currencyCode.orEmpty()
    val locale = Locale.forLanguageTag(normalizeAppLanguageTag(language))
    val currency = currencyCode
        ?.let { code -> runCatching { Currency.getInstance(code) }.getOrNull() }
        ?: return amount.toString()
    val scale = currency.defaultFractionDigits.coerceAtLeast(0)
    val decimalAmount = BigDecimal.valueOf(amount, scale)
    val numberFormatter = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = scale
    }
    val amountText = numberFormatter.format(decimalAmount)
    val symbol = currency.getSymbol(locale)
    val currencyText = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        minimumFractionDigits = 0
        maximumFractionDigits = scale
    }.format(decimalAmount)
    val symbolIndex = currencyText.indexOf(symbol)
    val digitIndex = currencyText.indexOfFirst(Char::isDigit)

    return if (symbolIndex >= 0 && (digitIndex < 0 || symbolIndex < digitIndex)) {
        "$symbol $amountText"
    } else {
        "$amountText $symbol"
    }
}

private fun attendanceOptionSummaryLabel(
    option: EventAttendanceOption,
    language: String,
    localizer: AppTextProvider
): String =
    when (option.type) {
        EventAttendanceType.Physical ->
            option.displayName?.resolve(language)?.takeIf(String::isNotBlank)
                ?: option.address?.resolve(language).orEmpty()
        EventAttendanceType.Online -> localizer.get("AttendanceOnline", language)
        EventAttendanceType.Television -> localizer.get("AttendanceTelevision", language)
    }

fun String.removeDiacritics(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}
