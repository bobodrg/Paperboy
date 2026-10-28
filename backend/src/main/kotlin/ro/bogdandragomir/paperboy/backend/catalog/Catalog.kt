package ro.bogdandragomir.paperboy.backend.catalog

import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Topic

data class Feed(
    val url: String,
    val sourceId: String,
    val topicId: String,
)

/**
 * The topics users can follow and the RSS feeds we read for each one.
 * Kept in code so every change is reviewed in a pull request; it is upserted into the database on startup.
 */
object Catalog {
    val topics =
        listOf(
            Topic("tech", "Tech"),
            Topic("ai", "AI", parentId = "tech"),
            Topic("science", "Science"),
            Topic("world", "World"),
            Topic("business", "Business"),
            Topic("sport", "Sport"),
            Topic("gaming", "Gaming"),
            Topic("health", "Health"),
            Topic("climate", "Climate"),
        )

    val sources =
        listOf(
            Source("the-verge", "The Verge", "https://www.theverge.com"),
            Source("ars-technica", "Ars Technica", "https://arstechnica.com"),
            Source("techcrunch", "TechCrunch", "https://techcrunch.com"),
            Source("bbc", "BBC News", "https://www.bbc.com/news"),
            Source("guardian", "The Guardian", "https://www.theguardian.com"),
            Source("al-jazeera", "Al Jazeera", "https://www.aljazeera.com"),
            Source("science-daily", "ScienceDaily", "https://www.sciencedaily.com"),
            Source("polygon", "Polygon", "https://www.polygon.com"),
            Source("eurogamer", "Eurogamer", "https://www.eurogamer.net"),
        )

    val feeds =
        listOf(
            Feed("https://www.theverge.com/rss/index.xml", "the-verge", "tech"),
            Feed("https://feeds.arstechnica.com/arstechnica/index", "ars-technica", "tech"),
            Feed("https://techcrunch.com/feed/", "techcrunch", "tech"),
            Feed("https://www.theguardian.com/technology/rss", "guardian", "tech"),
            Feed("https://techcrunch.com/category/artificial-intelligence/feed/", "techcrunch", "ai"),
            Feed("https://www.theverge.com/rss/ai-artificial-intelligence/index.xml", "the-verge", "ai"),
            Feed("https://feeds.arstechnica.com/arstechnica/science", "ars-technica", "science"),
            Feed("https://www.sciencedaily.com/rss/all.xml", "science-daily", "science"),
            Feed("https://www.theguardian.com/science/rss", "guardian", "science"),
            Feed("https://feeds.bbci.co.uk/news/world/rss.xml", "bbc", "world"),
            Feed("https://www.theguardian.com/world/rss", "guardian", "world"),
            Feed("https://www.aljazeera.com/xml/rss/all.xml", "al-jazeera", "world"),
            Feed("https://feeds.bbci.co.uk/news/business/rss.xml", "bbc", "business"),
            Feed("https://www.theguardian.com/uk/business/rss", "guardian", "business"),
            Feed("https://feeds.bbci.co.uk/sport/rss.xml", "bbc", "sport"),
            Feed("https://www.theguardian.com/uk/sport/rss", "guardian", "sport"),
            Feed("https://www.polygon.com/rss/index.xml", "polygon", "gaming"),
            Feed("https://www.eurogamer.net/feed", "eurogamer", "gaming"),
            Feed("https://feeds.bbci.co.uk/news/health/rss.xml", "bbc", "health"),
            Feed("https://www.theguardian.com/environment/rss", "guardian", "climate"),
        )
}
