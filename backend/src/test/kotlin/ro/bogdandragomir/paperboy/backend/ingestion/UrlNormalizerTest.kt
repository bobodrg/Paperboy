package ro.bogdandragomir.paperboy.backend.ingestion

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlNormalizerTest {
    @Test
    fun removesTrackingParametersAndFragment() {
        assertEquals(
            "https://www.bbc.co.uk/news/world-1?page=2",
            UrlNormalizer.normalize("https://WWW.BBC.co.uk/news/world-1?utm_medium=rss&page=2&at_campaign=x#comments"),
        )
    }

    @Test
    fun dropsTheQuestionMarkWhenNoParametersRemain() {
        assertEquals("https://example.com/a", UrlNormalizer.normalize("https://example.com/a?utm_source=feed"))
    }

    @Test
    fun leavesUnparseableUrlsAlone() {
        assertEquals("not a url", UrlNormalizer.normalize(" not a url "))
    }
}
