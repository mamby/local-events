package net.mamby.events.core

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.mamby.events.testFeedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelBehaviorTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun feedQueryNormalize_migrates_legacy_location_scope_to_attendance_scope() {
        val query = FeedQuery(
            locationScope = FeedLocationScope(
                id = " Paris ",
                name = " Paris ",
                displayPath = " Paris, France ",
                isSearchable = true
            ),
            text = " music ",
            location = " rooftop ",
            category = " Music "
        )

        val normalized = query.normalize()

        assertEquals("paris", normalized.attendanceScopeId)
        assertEquals("paris", normalized.attendanceScope?.id)
        assertEquals("Paris", normalized.attendanceScope?.name)
        assertEquals("music", normalized.text)
        assertEquals("rooftop", normalized.location)
        assertEquals("Music", normalized.category)
    }

    @Test
    fun feedQueryMerge_keeps_existing_values_when_filter_is_empty() {
        val query = FeedQuery(
            text = "music",
            category = "Music",
            priceFilter = FeedPriceFilter.Free
        )

        val merged = query.merge(FeedQuery(location = "Paris"))

        assertEquals("music", merged.text)
        assertEquals("Music", merged.category)
        assertEquals("Paris", merged.location)
        assertEquals(FeedPriceFilter.Free, merged.priceFilter)
    }

    @Test
    fun localizedTextResolve_uses_neutral_language_then_default() {
        val text = LocalizedText(default = "Hello", translations = mapOf("fr" to "Bonjour"))

        assertEquals("Bonjour", text.resolve("fr-FR"))
        assertEquals("Hello", text.resolve("ar"))
        assertEquals("Hello", text.resolve(null))
    }

    @Test
    fun mediaTypeSerializer_accepts_name_and_legacy_numeric_values() {
        assertEquals(MediaType.Image, json.decodeFromString<MediaType>("\"Image\""))
        assertEquals(MediaType.Video, json.decodeFromString<MediaType>("\"video\""))
        assertEquals(MediaType.Image, json.decodeFromString<MediaType>("\"0\""))
        assertEquals(MediaType.Video, json.decodeFromString<MediaType>("\"1\""))
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<MediaType>("\"audio\"")
        }
    }

    @Test
    fun eventFeedItem_readsLegacySavedDataWithoutStructuredScheduleOrPrices() {
        val legacyFields = json.encodeToJsonElement(EventFeedItem.serializer(), testFeedItem())
            .jsonObject
            .toMutableMap()
            .apply {
                remove("occurrences")
                remove("priceOptions")
            }

        val item = json.decodeFromJsonElement(EventFeedItem.serializer(), JsonObject(legacyFields))

        assertEquals(emptyList<EventOccurrence>(), item.occurrences)
        assertEquals(emptyList<EventPriceOption>(), item.priceOptions)
        assertEquals("0 $/gratuit", item.priceLabel)
    }

    @Test
    fun normalizeAppLanguageTag_supports_expected_languages_only() {
        assertEquals("fr", normalizeAppLanguageTag("fr-CA"))
        assertEquals("ar", normalizeAppLanguageTag("ar_MA"))
        assertEquals("de", normalizeAppLanguageTag("de-DE"))
        assertEquals("pt", normalizeAppLanguageTag("pt-BR"))
        assertEquals("zh-Hans", normalizeAppLanguageTag("zh-CN"))
        assertEquals("en", normalizeAppLanguageTag("zh-TW"))
        assertEquals("en", normalizeAppLanguageTag(null))
    }

    @Test
    fun emptyQuery_is_false_after_normalization_when_scope_exists() {
        val query = FeedQuery(
            attendanceScopeId = " paris ",
            text = " "
        ).normalize()

        assertFalse(query.isEmpty)
        assertNull(query.text)
    }
}
