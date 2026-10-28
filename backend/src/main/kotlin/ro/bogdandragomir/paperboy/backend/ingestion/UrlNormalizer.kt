package ro.bogdandragomir.paperboy.backend.ingestion

import java.net.URI

/**
 * Canonical form of an article URL, so the same story reached through different
 * feeds or tracking links is stored once.
 */
object UrlNormalizer {
    private val trackingParams =
        setOf(
            "fbclid",
            "gclid",
            "mc_cid",
            "mc_eid",
            "cmp",
            "ns_mchannel",
            "ns_source",
            "ns_campaign",
            "at_medium",
            "at_campaign",
        )

    fun normalize(url: String): String {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return url.trim()
        val query =
            uri.rawQuery
                ?.split('&')
                ?.filter { param ->
                    val key = param.substringBefore('=').lowercase()
                    key.isNotEmpty() && !key.startsWith("utm_") && key !in trackingParams
                }?.takeIf { it.isNotEmpty() }
                ?.joinToString("&")

        return buildString {
            append(uri.scheme?.lowercase() ?: "https")
            append("://")
            append(uri.rawAuthority?.lowercase().orEmpty())
            append(uri.rawPath.orEmpty())
            if (query != null) append('?').append(query)
        }
    }
}
