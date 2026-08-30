package com.zaaam.nettra.search

import java.net.URLEncoder

/**
 * FR-2: Address Bar & Default Search = DuckDuckGo — P0
 * Stateless detection: URL vs query, build DuckDuckGo search URL.
 * Contract: 02_ARCHITECTURE.md core-search
 */
object SearchRouter {

    private const val DDG_SEARCH_BASE = "https://duckduckgo.com/?q="
    private const val DDG_SOURCE_PARAM = "&t=nettra"

    /**
     * Very conservative URL detection to avoid misclassifying queries as URLs.
     * Accepts: http(s) colon slash slash, localhost, and domain-like with dot + valid TLD, no spaces.
     */
    fun isValidUrl(input: String): Boolean {
        val s = input.trim()
        if (s.isEmpty()) return false
        if (s.contains(' ')) return false
        // scheme present
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) {
            return try {
                val u = java.net.URL(s)
                u.host.isNotEmpty()
            } catch (_: Exception) { false }
        }
        if (s.matches(Regex("""^localhost(:\d+)?(/.*)?$""", RegexOption.IGNORE_CASE))) return true
        // domain-like: foo.bar or foo.bar/baz
        if (s.matches(Regex("""^[a-z0-9.-]+\.[a-z]{2,}(/.*)?$""", RegexOption.IGNORE_CASE))) return true
        return false
    }

    fun buildSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return "$DDG_SEARCH_BASE$encoded$DDG_SOURCE_PARAM"
    }

    /**
     * Resolve input to final navigation URL.
     * @return URL to load in WebView
     */
    fun resolve(input: String): String {
        val trimmed = input.trim()
        return if (isValidUrl(trimmed)) {
            if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) trimmed
            else "https://$trimmed"
        } else {
            buildSearchUrl(trimmed)
        }
    }
}
