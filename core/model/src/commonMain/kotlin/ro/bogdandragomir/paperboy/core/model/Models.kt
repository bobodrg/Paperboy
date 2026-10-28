package ro.bogdandragomir.paperboy.core.model

import kotlinx.serialization.Serializable

/** A subject the user can follow, e.g. "Tech" or its sub-topic "AI". */
@Serializable
data class Topic(
    val id: String,
    val name: String,
    val parentId: String? = null,
)

/** A news outlet we ingest from. */
@Serializable
data class Source(
    val id: String,
    val name: String,
    val homepageUrl: String,
)

/**
 * One story in a brief. Several articles about the same event are clustered
 * into a single story, so [sourceCount] can be greater than one.
 */
@Serializable
data class Story(
    val id: String,
    val headline: String,
    val summary: String,
    val imageUrl: String? = null,
    val url: String,
    val source: Source,
    val topicIds: List<String>,
    val publishedAtEpochMillis: Long,
    val sourceCount: Int = 1,
)

/** The finite list of stories a user gets for one day. */
@Serializable
data class Brief(
    val date: String,
    val stories: List<Story>,
)
