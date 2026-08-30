package com.zaaam.nettra.search

import java.net.URLEncoder

/**
 * FR-2: Address Bar & Default Search = DuckDuckGo — P0
 * Stateless detection: URL vs query, build DuckDuckGo search URL.
 * Contract: 02_ARCHITECTURE.md core-search
 */
object SearchRouter {

    private const val DDG_SEARCH_BASE = "https://duckduckgo.com/?q="
    private const val DDG_SOURCE_PARAM = "" // hapus &t=nettra — fix ERR_SSL_VERSION_OR_CIPHER_MISMATCH FR-2

    /**
     * Very conservative URL detection to avoid misclassifying queries as URLs.
     * Accepts: http(s) colon slash slash, localhost, and domain-like with dot + valid TLD, no spaces.
     */
    fun isValidUrl(input: String): Boolean {
        val s = input.trim()
        if (s.isEmpty()) return false
        if (s.contains(' ')) return false
        // scheme present — validate host and port range 1..65535
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) {
            return try {
                val u = java.net.URL(s)
                if (u.host.isEmpty()) return false
                val p = u.port
                if (p != -1 && (p < 1 || p > 65535)) return false
                true
            } catch (_: Exception) { false }
        }
        // localhost with optional port 1-65535 and optional path
        if (s.matches(Regex("""^localhost(:\d{1,5})?(/.*)?$""", RegexOption.IGNORE_CASE))) {
            val m = Regex("""^localhost:(\d{1,5})""", RegexOption.IGNORE_CASE).find(s)
            if (m != null) {
                val port = m.groupValues[1].toIntOrNull() ?: return false
                if (port < 1 || port > 65535) return false
            }
            return true
        }
        // domain-like with optional port and path: regex ensures structure, then label/port validation
        if (!s.matches(Regex("""^[a-z0-9.-]+\.[a-z]{2,}(:\d{1,5})?(/.*)?$""", RegexOption.IGNORE_CASE))) {
            return false
        }
        val slashIndex = s.indexOf('/')
        val hostPort = if (slashIndex != -1) s.substring(0, slashIndex) else s
        val colonIndex = hostPort.lastIndexOf(':')
        val host: String
        if (colonIndex != -1) {
            val portStr = hostPort.substring(colonIndex + 1)
            if (!portStr.matches(Regex("""\d{1,5}"""))) return false
            val port = portStr.toIntOrNull() ?: return false
            if (port < 1 || port > 65535) return false
            host = hostPort.substring(0, colonIndex)
            if (host.isEmpty()) return false
        } else {
            host = hostPort
        }
        if (host.contains("..")) return false
        if (host.startsWith(".") || host.startsWith("-") || host.endsWith(".") || host.endsWith("-")) return false
        val labels = host.split(".")
        if (labels.size < 2) return false
        for (label in labels) {
            if (label.isEmpty()) return false
            if (label.startsWith("-") || label.endsWith("-")) return false
        }
        val tld = labels.last()
        if (!tld.matches(Regex("""^[a-z]{2,}$""", RegexOption.IGNORE_CASE))) return false
        val labelRegex = Regex("""^[a-z0-9]([a-z0-9-]*[a-z0-9])?$""", RegexOption.IGNORE_CASE)
        for (i in 0 until labels.size - 1) {
            if (!labels[i].matches(labelRegex)) return false
        }
        return true
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
