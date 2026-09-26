package net.mamby.events

import net.mamby.events.core.EventFeedItem
import net.mamby.events.core.EventAttendanceOption
import net.mamby.events.core.EventAttendanceType
import net.mamby.events.core.EventOccurrence
import net.mamby.events.core.EventPriceOption
import net.mamby.events.core.EventPriceType
import net.mamby.events.core.EventSource
import net.mamby.events.core.LocalizedText
import net.mamby.events.core.Media
import net.mamby.events.core.MediaType

fun testFeedItem(id: String = "00000000-0000-0000-0000-000000000001"): EventFeedItem =
    EventFeedItem(
        id = id,
        sequence = 1,
        media = Media(type = MediaType.Image, url = "https://example.com/image.jpg"),
        category = LocalizedText(default = "Music", translations = mapOf("fr" to "Musique")),
        title = LocalizedText(default = "Rooftop concert", translations = mapOf("fr" to "Concert")),
        description = LocalizedText(default = "Live set"),
        longDescription = LocalizedText(default = "An extended event description."),
        contextDetails = "Paris",
        locationScopeId = "paris",
        attendanceOptions = listOf(
            EventAttendanceOption(
                type = EventAttendanceType.Physical,
                displayName = LocalizedText(default = "Paris"),
                locationScopeId = "paris",
                isPrimary = true
            )
        ),
        startDate = "2026-06-01T20:00:00Z",
        occurrences = listOf(EventOccurrence(startsAt = "2026-06-01T20:00:00Z")),
        publishedAt = "2026-05-01T12:00:00Z",
        updatedAt = "2026-05-01T12:00:00Z",
        priceLabel = "0 $/gratuit",
        priceOptions = listOf(EventPriceOption(type = EventPriceType.Free)),
        source = EventSource(name = "Local Events", type = "Official")
    )
