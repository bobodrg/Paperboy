package ro.bogdandragomir.paperboy.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelsSerializationTest {
    @Test
    fun briefSurvivesJsonRoundTrip() {
        val brief =
            Brief(
                date = "2026-10-28",
                stories =
                    listOf(
                        Story(
                            id = "s1",
                            headline = "Headline",
                            summary = "Summary",
                            url = "https://example.com/a",
                            source = Source("src", "Example", "https://example.com"),
                            topicIds = listOf("tech"),
                            publishedAtEpochMillis = 0,
                        ),
                    ),
            )

        val json = Json.encodeToString(Brief.serializer(), brief)

        assertEquals(brief, Json.decodeFromString(Brief.serializer(), json))
    }
}
