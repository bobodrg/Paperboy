package ro.bogdandragomir.paperboy.backend.news

/** Turns what a reader types into a safe PostgreSQL tsquery. */
object SearchQuery {
    private const val MAX_TERMS = 6
    private val word = Regex("[\\p{L}\\p{N}]+")

    /**
     * "apple vision p" → "apple & vision & p:*": every word must match, and the last one may be unfinished.
     * Only letters and digits survive, so nothing the reader types can change the query's syntax.
     * Returns null when there's nothing to search for.
     */
    fun toTsQuery(input: String): String? {
        val terms =
            word
                .findAll(input.lowercase())
                .map { it.value }
                .take(MAX_TERMS)
                .toList()
        if (terms.isEmpty()) return null
        return terms.dropLast(1).plus("${terms.last()}:*").joinToString(" & ")
    }

    /** Escapes LIKE wildcards so "%" or "_" in the input match literally. */
    fun escapeLike(input: String): String = input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
