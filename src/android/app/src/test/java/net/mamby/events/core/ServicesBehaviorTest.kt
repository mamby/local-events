package net.mamby.events.core

import net.mamby.events.testFeedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicesBehaviorTest {
    private val localizer = object : AppTextProvider {
        override fun get(key: String, language: String): String =
            when (key) {
                "FilterFree" -> when (language) {
                    "fr" -> "Gratuit"
                    "ar" -> "مجاني"
                    else -> "Free"
                }
                else -> key
            }

        override fun format(key: String, language: String, vararg args: Any?): String =
            args.foldIndexed(key) { index, current, value ->
                current.replace("{$index}", value?.toString().orEmpty())
            }
    }
    private val formatter = AppDateFormatter(localizer)

    @Test
    fun feedDate_usesDateOnlyLocalizedFormat() {
        val augustDate = "2025-08-19T18:00:00+02:00"
        val januaryDate = "2025-01-07T18:00:00+01:00"

        assertEquals("Aug 19, 2025", formatter.formatFeedDate(augustDate, "en"))
        assertTrue(formatter.formatFeedDate(januaryDate, "fr").contains("janv."))
        assertTrue(formatter.formatFeedDate(augustDate, "ar").isNotBlank())
        assertFalse(formatter.formatFeedDate(januaryDate, "fr").contains(":"))
    }

    @Test
    fun priceSummary_usesLocaleSpecificCurrencyPosition() {
        val item = testFeedItem().copy(
            priceOptions = listOf(
                EventPriceOption(
                    type = EventPriceType.Paid,
                    amountMinorUnits = 1800,
                    currencyCode = "EUR"
                )
            )
        )

        assertEquals("€ 18", priceSummaryText(item, "en", localizer).primaryText)
        assertEquals("18 €", priceSummaryText(item, "fr", localizer).primaryText)
    }

    @Test
    fun occurrenceDetails_includeEndTimeAndSpanningEndDate() {
        val start = "2025-08-19T18:00:00+02:00"

        assertEquals(
            "Aug 19, 2025, 6:00\u202fPM - 9:00\u202fPM",
            formatter.formatOccurrence(start, "2025-08-19T21:00:00+02:00", "en")
        )
        assertEquals(
            "Aug 19, 2025, 6:00\u202fPM - Aug 20, 2025, 1:00\u202fAM",
            formatter.formatOccurrence(start, "2025-08-20T01:00:00+02:00", "en")
        )
    }
}
