package ro.bogdandragomir.paperboy.brief

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story

interface BriefRepository {
    /** Today's brief. Emits again whenever the local copy changes. */
    fun observeTodayBrief(): Flow<Brief>
}

/**
 * Placeholder until the backend and the local database exist (roadmap phases 2 and 4).
 * The stories are invented sample data.
 */
class SampleBriefRepository : BriefRepository {
    override fun observeTodayBrief(): Flow<Brief> = flowOf(sampleBrief)

    private companion object {
        val sampleSource = Source(id = "sample", name = "Sample Daily", homepageUrl = "https://example.com")

        val sampleBrief =
            Brief(
                date = "2026-10-28",
                stories =
                    listOf(
                        sampleStory(
                            "1",
                            "Your topics, ranked for this morning",
                            "This is where today's most relevant story will appear, with a short summary from the publisher.",
                        ),
                        sampleStory(
                            "2",
                            "One story, several sources",
                            "When many outlets cover the same event, Paperboy shows it once and tells you how many sources covered it.",
                            sourceCount = 4,
                        ),
                        sampleStory(
                            "3",
                            "Five minutes, then you're done",
                            "The brief is finite on purpose. Read it with your coffee and get on with your day.",
                        ),
                    ),
            )

        fun sampleStory(
            id: String,
            headline: String,
            summary: String,
            sourceCount: Int = 1,
        ) = Story(
            id = id,
            headline = headline,
            summary = summary,
            url = "https://example.com/$id",
            source = sampleSource,
            topicIds = listOf("tech"),
            publishedAtEpochMillis = 0,
            sourceCount = sourceCount,
        )
    }
}
