package ro.bogdandragomir.paperboy.backend.ingestion

import com.rometools.modules.mediarss.MediaEntryModule
import com.rometools.modules.mediarss.MediaModule
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.time.Instant

data class ParsedArticle(
    val url: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val publishedAt: Instant,
)

/** Turns RSS or Atom bytes into articles. Entries without a title or link are dropped. */
class FeedParser(
    private val now: () -> Instant = Instant::now,
) {
    fun parse(bytes: ByteArray): List<ParsedArticle> {
        val feed = SyndFeedInput().build(XmlReader(ByteArrayInputStream(bytes)))
        return feed.entries.mapNotNull(::toArticle)
    }

    private fun toArticle(entry: SyndEntry): ParsedArticle? {
        val title = entry.title?.let(::plainText)?.takeIf { it.isNotBlank() } ?: return null
        val link = entry.link?.trim()?.takeIf { it.startsWith("http") } ?: return null
        val description = entry.description?.value.orEmpty()
        val content = entry.contents.joinToString(" ") { it.value.orEmpty() }
        val published = (entry.publishedDate ?: entry.updatedDate)?.toInstant() ?: now()

        return ParsedArticle(
            url = UrlNormalizer.normalize(link),
            title = title,
            summary = truncate(plainText(description.ifBlank { content }), MAX_SUMMARY_LENGTH),
            // Atom feeds (e.g. The Verge) often keep images only in the full content.
            imageUrl = imageOf(entry, "$description $content"),
            // Some feeds post-date entries; never let a story claim to be from the future.
            publishedAt = minOf(published, now()),
        )
    }

    private fun imageOf(
        entry: SyndEntry,
        html: String,
    ): String? {
        val media = entry.getModule(MediaModule.URI) as? MediaEntryModule
        val fromMedia =
            media
                ?.mediaContents
                ?.firstOrNull { it.reference != null && it.medium != "video" }
                ?.reference
                ?.toString()
                ?: media
                    ?.metadata
                    ?.thumbnail
                    ?.firstOrNull()
                    ?.url
                    ?.toString()
                ?: media
                    ?.mediaGroups
                    ?.flatMap { it.contents.toList() }
                    ?.firstOrNull()
                    ?.reference
                    ?.toString()
        val fromEnclosure = entry.enclosures.firstOrNull { it.type?.startsWith("image/") == true }?.url
        val fromHtml =
            Jsoup
                .parse(html)
                .selectFirst("img[src]")
                ?.absUrl("src")
                ?.takeIf { it.isNotBlank() }
        return (fromMedia ?: fromEnclosure ?: fromHtml)?.takeIf { it.startsWith("http") }
    }

    private fun plainText(html: String): String = Jsoup.parse(html).text().trim()

    private fun truncate(
        text: String,
        max: Int,
    ): String {
        if (text.length <= max) return text
        val cut = text.take(max).substringBeforeLast(' ')
        return "$cut…"
    }

    private companion object {
        const val MAX_SUMMARY_LENGTH = 280
    }
}
