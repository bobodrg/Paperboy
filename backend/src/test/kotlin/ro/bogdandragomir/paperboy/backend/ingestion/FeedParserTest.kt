package ro.bogdandragomir.paperboy.backend.ingestion

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedParserTest {
    private val now = Instant.parse("2026-10-28T10:00:00Z")
    private val articles =
        FeedParser { now }.parse(
            requireNotNull(javaClass.getResourceAsStream("/feeds/sample-rss.xml")).readBytes(),
        )

    @Test
    fun dropsEntriesWithoutATitle() {
        assertEquals(3, articles.size)
    }

    @Test
    fun mapsTheFieldsAndCleansTheUrl() {
        val chips = articles.first()

        assertEquals("Chip makers report record quarter", chips.title)
        assertEquals("https://example.com/news/chips?id=42", chips.url)
        assertEquals("Demand for AI chips kept growing.", chips.summary)
        assertEquals("https://example.com/chips.jpg", chips.imageUrl)
        assertEquals(Instant.parse("2026-10-27T08:00:00Z"), chips.publishedAt)
    }

    @Test
    fun fallsBackToTheFirstImageInTheDescription() {
        assertEquals("https://example.com/inline.jpg", articles[1].imageUrl)
    }

    @Test
    fun clampsFutureDatesToNow() {
        assertEquals(now, articles[2].publishedAt)
    }
}
