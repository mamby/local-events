package net.mamby.events.core

import java.util.Locale
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

@Serializable(with = MediaTypeSerializer::class)
enum class MediaType {
    Image,
    Video
}

@Serializable
enum class FeedPriceFilter {
    Any,
    Free,
    Paid
}

@Serializable
enum class EventAttendanceType {
    Physical,
    Online,
    Television
}

@Serializable
enum class EventPriceType {
    Free,
    Paid
}

@Serializable
enum class FeedAttendanceScopeType {
    Geographic,
    Online,
    Television
}

@Serializable
enum class FeedFilterKind {
    Location,
    Category,
    DateRange,
    Price,
    Accessibility,
    AgeRestriction
}

@Serializable
enum class TelemetryInteractionAction {
    @SerialName("view")
    View,

    @SerialName("favorite_added")
    FavoriteAdded,

    @SerialName("favorite_removed")
    FavoriteRemoved,

    @SerialName("share_intent")
    ShareIntent
}

@Serializable
enum class AppLanguagePreference {
    System,
    English,
    French,
    Arabic
}

data class AppLanguage(
    val tag: String
)

val SupportedAppLanguages: List<AppLanguage> = listOf(
    AppLanguage("en"),
    AppLanguage("fr"),
    AppLanguage("ar"),
    AppLanguage("es"),
    AppLanguage("pt"),
    AppLanguage("de"),
    AppLanguage("zh-Hans"),
    AppLanguage("hi"),
    AppLanguage("id"),
    AppLanguage("ja"),
    AppLanguage("ko"),
    AppLanguage("it"),
    AppLanguage("tr"),
    AppLanguage("ru"),
    AppLanguage("nl"),
    AppLanguage("pl"),
    AppLanguage("vi"),
    AppLanguage("th")
)

@Serializable
enum class AppThemePreference {
    System,
    Light,
    Dark
}

@Serializable
data class LocalizedText(
    val default: String,
    val translations: Map<String, String> = emptyMap()
) {
    fun resolve(language: String?): String {
        if (language.isNullOrBlank()) {
            return default
        }

        translations[language]?.takeIf(String::isNotBlank)?.let { return it }
        val neutralLanguage = language.split('-', '_').firstOrNull().orEmpty()
        return translations[neutralLanguage]?.takeIf(String::isNotBlank)
            ?: translations["en"]?.takeIf(String::isNotBlank)
            ?: default
    }
}

@Serializable
data class EventSource(
    val name: String,
    val link: String? = null,
    val type: String
)

@Serializable
data class EventAttendanceOption(
    val type: EventAttendanceType,
    val displayName: LocalizedText? = null,
    val address: LocalizedText? = null,
    val locationScopeId: String? = null,
    val isPrimary: Boolean = false
)

@Serializable
data class EventOccurrence(
    val startsAt: String,
    val endsAt: String? = null
)

@Serializable
data class EventPriceOption(
    val type: EventPriceType,
    val amountMinorUnits: Long? = null,
    val currencyCode: String? = null,
    val condition: LocalizedText? = null
)

@Serializable
data class Media(
    val type: MediaType,
    val url: String,
    val thumbnailUrl: String? = null,
    val audioUrl: String? = null,
    val durationSeconds: Int? = null
) {
    val displayUrl: String
        get() = thumbnailUrl?.takeIf(String::isNotBlank) ?: url

    val isVideo: Boolean
        get() = type == MediaType.Video

    val hasAudio: Boolean
        get() = isVideo || !audioUrl.isNullOrBlank()

    val hasPlayback: Boolean
        get() = isVideo || hasAudio

    val playbackUrl: String?
        get() = if (isVideo) url else audioUrl
}

@Serializable
data class AgeRestriction(
    val minimumRequiredAge: Int? = null
)

@Serializable
data class RecommendedAge(
    val minimumAge: Int,
    val maximumAge: Int? = null
)

@Serializable
data class EventFeedItem(
    val id: String,
    val sequence: Int = 0,
    val media: Media,
    val category: LocalizedText,
    val title: LocalizedText,
    val description: LocalizedText,
    val longDescription: LocalizedText? = null,
    val accessibility: LocalizedText? = null,
    val ageRestriction: AgeRestriction? = null,
    val recommendedAge: RecommendedAge? = null,
    val contextDetails: String = "",
    val locationScopeId: String = "",
    val attendanceOptions: List<EventAttendanceOption> = emptyList(),
    val startDate: String,
    val occurrences: List<EventOccurrence> = emptyList(),
    val publishedAt: String,
    val updatedAt: String,
    val priceLabel: String,
    val priceOptions: List<EventPriceOption> = emptyList(),
    val source: EventSource,
    val isFavorited: Boolean = false
)

@Serializable
data class TelemetrySessionResponse(
    val sessionToken: String
)

@Serializable
data class TelemetryInteractionRequest(
    val eventId: String,
    val action: TelemetryInteractionAction
)

@Serializable
data class QueuedTelemetryInteraction(
    val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val action: TelemetryInteractionAction,
    val enqueuedAt: String
) {
    fun toRequest(): TelemetryInteractionRequest =
        TelemetryInteractionRequest(eventId = eventId, action = action)
}

@Serializable
data class FeedLocationScope(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val displayPath: String,
    val isSearchable: Boolean = false,
    val aliases: List<String> = emptyList()
) {
    fun normalize(): FeedLocationScope {
        val normalizedId = normalizeId(id)
        return copy(
            id = normalizedId,
            name = name.trim(),
            parentId = parentId?.trim()?.takeIf(String::isNotBlank),
            displayPath = displayPath.trim(),
            aliases = aliases.map(String::trim).filter(String::isNotBlank)
        )
    }

    companion object {
        fun normalizeId(value: String?): String =
            value?.trim()?.lowercase().orEmpty()
    }
}

@Serializable
data class FeedAttendanceScope(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val displayPath: String,
    val isSearchable: Boolean = false,
    val aliases: List<String> = emptyList(),
    val type: FeedAttendanceScopeType = FeedAttendanceScopeType.Geographic
) {
    fun normalize(): FeedAttendanceScope {
        val normalizedId = normalizeId(id)
        return copy(
            id = normalizedId,
            name = name.trim(),
            parentId = parentId?.trim()?.takeIf(String::isNotBlank),
            displayPath = displayPath.trim(),
            aliases = aliases.map(String::trim).filter(String::isNotBlank)
        )
    }

    companion object {
        const val WorldId = "world"
        const val OnlineId = "online"
        const val TelevisionId = "tv"

        fun normalizeId(value: String?): String =
            value?.trim()?.lowercase().orEmpty()
    }
}

fun FeedLocationScope.toAttendanceScope(): FeedAttendanceScope =
    FeedAttendanceScope(
        id = id,
        name = name,
        parentId = parentId,
        displayPath = displayPath,
        isSearchable = isSearchable,
        aliases = aliases
    )

@Serializable
data class FeedCategoryOption(
    val value: String,
    val label: String
) {
    fun normalize(): FeedCategoryOption =
        copy(value = value.trim(), label = label.trim())
}

@Serializable
data class FeedTermOption(
    val value: String,
    val label: String
) {
    fun normalize(): FeedTermOption =
        copy(value = value.trim(), label = label.trim())
}

@Serializable
data class FeedQuery(
    val terms: List<String> = emptyList(),
    val attendanceScopeIds: List<String> = emptyList(),
    val attendanceScopes: List<FeedAttendanceScope> = emptyList(),
    val locations: List<String> = emptyList(),
    val attendanceScopeId: String = "",
    val attendanceScope: FeedAttendanceScope? = null,
    val locationScopeId: String = "",
    val locationScope: FeedLocationScope? = null,
    val text: String? = null,
    val location: String? = null,
    val category: String? = null,
    val accessibility: String? = null,
    val accessibleForAge: Int? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val priceFilter: FeedPriceFilter = FeedPriceFilter.Any
) {
    val effectiveTerms: List<String>
        get() = (terms + listOfNotNull(text))
            .normalizeTextValues()

    val effectiveAttendanceScopes: List<FeedAttendanceScope>
        get() = (attendanceScopes + listOfNotNull(attendanceScope ?: locationScope?.toAttendanceScope()))
            .asSequence()
            .map(FeedAttendanceScope::normalize)
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id.lowercase() }
            .toList()

    val effectiveAttendanceScopeIds: List<String>
        get() = (
            attendanceScopeIds +
                listOf(attendanceScopeId, locationScopeId) +
                effectiveAttendanceScopes.map(FeedAttendanceScope::id)
            )
            .asSequence()
            .map(FeedAttendanceScope::normalizeId)
            .filter(String::isNotBlank)
            .distinct()
            .toList()

    val effectiveLocations: List<String>
        get() = (locations + listOfNotNull(location))
            .normalizeTextValues()

    val effectiveAttendanceScope: FeedAttendanceScope?
        get() = effectiveAttendanceScopes.firstOrNull()

    val effectiveAttendanceScopeId: String
        get() = effectiveAttendanceScopeIds.firstOrNull().orEmpty()

    val isEmpty: Boolean
        get() = effectiveAttendanceScopeIds.isEmpty() &&
            effectiveTerms.isEmpty() &&
            effectiveLocations.isEmpty() &&
            category.isNullOrBlank() &&
            accessibility.isNullOrBlank() &&
            accessibleForAge == null &&
            dateFrom.isNullOrBlank() &&
            dateTo.isNullOrBlank() &&
            priceFilter == FeedPriceFilter.Any

    fun normalize(): FeedQuery {
        val normalizedTerms = effectiveTerms
        val normalizedScopes = effectiveAttendanceScopes
        val normalizedScopeIds = effectiveAttendanceScopeIds
        val normalizedLocations = effectiveLocations

        return copy(
            terms = normalizedTerms,
            attendanceScopeIds = normalizedScopeIds,
            attendanceScopes = normalizedScopes,
            locations = normalizedLocations,
            attendanceScopeId = normalizedScopeIds.firstOrNull().orEmpty(),
            attendanceScope = normalizedScopes.firstOrNull(),
            locationScopeId = "",
            locationScope = null,
            text = normalizedTerms.firstOrNull(),
            location = normalizedLocations.firstOrNull(),
            category = category.normalizeText(),
            accessibility = accessibility.normalizeText(),
            dateFrom = dateFrom.normalizeText(),
            dateTo = dateTo.normalizeText()
        )
    }

    fun merge(filter: FeedQuery): FeedQuery {
        val normalized = normalize()
        return normalized.copy(
            terms = normalized.terms + filter.effectiveTerms,
            attendanceScopeIds = normalized.attendanceScopeIds + filter.effectiveAttendanceScopeIds,
            attendanceScopes = normalized.attendanceScopes + filter.effectiveAttendanceScopes,
            locations = normalized.locations + filter.effectiveLocations,
            category = filter.category.normalizeText() ?: normalized.category,
            accessibility = filter.accessibility.normalizeText() ?: normalized.accessibility,
            accessibleForAge = filter.accessibleForAge ?: normalized.accessibleForAge,
            dateFrom = filter.dateFrom.normalizeText() ?: normalized.dateFrom,
            dateTo = filter.dateTo.normalizeText() ?: normalized.dateTo,
            priceFilter = if (filter.priceFilter == FeedPriceFilter.Any) normalized.priceFilter else filter.priceFilter
        ).normalize()
    }
}

@Serializable
data class FeedFilterChip(
    val kind: FeedFilterKind,
    val label: String,
    val query: FeedQuery = FeedQuery()
)

@Serializable
data class FeedPageResponse(
    val items: List<EventFeedItem>,
    val generatedAt: String,
    val refreshCursor: String? = null,
    val newSinceCount: Int = 0
)

@Serializable
data class FeedSnapshot(
    val language: String,
    val query: FeedQuery,
    val page: Int,
    val pageSize: Int,
    val response: FeedPageResponse,
    val cachedAt: String
)

@Serializable
data class FeedSnapshotCacheEntry(
    val version: Int,
    val snapshot: FeedSnapshot
)

@Serializable
data class FeedSearchHistoryEntry(
    val query: FeedQuery? = null,
    val attendanceScopeId: String = "",
    val attendanceScope: FeedAttendanceScope? = null,
    val locationScopeId: String = "",
    val locationScope: FeedLocationScope? = null,
    val text: String? = null,
    val filters: List<FeedFilterChip> = emptyList(),
    val lastUsedAt: String
)

@Serializable
data class FeedSearchDraft(
    val query: FeedQuery,
    val savedAt: String
)

data class FeedSearchParseResult(
    val textQuery: String?,
    val suggestions: List<FeedFilterChip>
)

data class SettingsState(
    val floatingSurfaceOpacityLevel: Float = 0f,
    val selectedLanguageTag: String? = null,
    val themePreference: AppThemePreference = AppThemePreference.System,
    val mediaAutoplayEnabled: Boolean = false,
    val householdChildAges: List<Int> = emptyList(),
    val systemLanguageTag: String = "en",
    val effectiveLanguage: String = "en"
)

fun normalizeAppLanguageTag(language: String?): String =
    when (val normalized = language?.trim()?.replace('_', '-')?.takeIf(String::isNotBlank)) {
        null -> "en"
        else -> when (normalized.split('-').firstOrNull()?.lowercase(Locale.ROOT)) {
            "ar" -> "ar"
            "de" -> "de"
            "en" -> "en"
            "es" -> "es"
            "fr" -> "fr"
            "hi" -> "hi"
            "id", "in" -> "id"
            "it" -> "it"
            "ja" -> "ja"
            "ko" -> "ko"
            "nl" -> "nl"
            "pl" -> "pl"
            "pt" -> "pt"
            "ru" -> "ru"
            "th" -> "th"
            "tr" -> "tr"
            "vi" -> "vi"
            "zh" -> if (normalized.isSimplifiedChineseTag()) "zh-Hans" else "en"
            else -> "en"
        }
    }

private fun String.isSimplifiedChineseTag(): Boolean {
    val parts = split('-').drop(1).map { it.lowercase(Locale.ROOT) }
    return parts.any { it == "hans" } || parts.any { it in SimplifiedChineseRegions }
}

private val SimplifiedChineseRegions = setOf("cn", "sg")

fun isSupportedAppLanguageTag(language: String?): Boolean =
    normalizeAppLanguageTag(language).let { normalized ->
        SupportedAppLanguages.any { it.tag == normalized }
    }

private fun String?.normalizeText(): String? =
    this?.trim()?.takeIf(String::isNotBlank)

private fun Iterable<String>.normalizeTextValues(): List<String> =
    asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy(String::lowercase)
        .toList()

object MediaTypeSerializer : KSerializer<MediaType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MediaType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MediaType) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): MediaType {
        val primitive = (decoder as? JsonDecoder)?.decodeJsonElement() as? JsonPrimitive
        val token = primitive?.content ?: decoder.decodeString()

        return when {
            token.equals(MediaType.Image.name, ignoreCase = true) || token == "0" -> MediaType.Image
            token.equals(MediaType.Video.name, ignoreCase = true) || token == "1" -> MediaType.Video
            else -> throw SerializationException("Unsupported media type value: $token")
        }
    }
}

internal fun normalizeFloatingSurfaceOpacityLevel(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 100f) else 0f
