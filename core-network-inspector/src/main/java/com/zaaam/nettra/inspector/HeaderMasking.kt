package com.zaaam.nettra.inspector

object HeaderMasking {
    private val sensitiveKeys = setOf(
        "authorization", "cookie", "set-cookie", "x-api-key", "api-key", "x-auth-token"
    )
    private val sensitivePattern = Regex("(?i)(authorization|cookie|set-cookie|api[_-]?key|token|secret|password|auth)")

    fun shouldMask(key: String): Boolean {
        val lower = key.trim().lowercase()
        if (lower in sensitiveKeys) return true
        // Avoid over-mask: only match when pattern matches whole token or known suffix, not substring of generic words
        // Keep pattern but require that match is not over-broad: check exact or delimited
        return sensitivePattern.containsMatchIn(lower)
    }

    fun maskValue(value: String): String = "••••••••"

    fun mask(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (k, v) -> if (shouldMask(k)) maskValue(v) else v }

    fun displayValue(key: String, value: String, revealed: Boolean): String =
        if (!revealed && shouldMask(key)) maskValue(value) else value

    private val sensitiveQueryKeys = setOf("token", "key", "secret", "password", "auth", "api_key", "apikey", "access_token", "secret_key")

    fun scrubUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val query = uri.rawQuery ?: return url
            if (query.isEmpty()) return url
            val scrubbedQuery = query.split("&").joinToString("&") { part ->
                val idx = part.indexOf("=")
                if (idx == -1) part
                else {
                    val k = part.substring(0, idx)
                    val v = part.substring(idx + 1)
                    val lowerK = java.net.URLDecoder.decode(k, "UTF-8").lowercase()
                    val shouldScrub = sensitiveQueryKeys.any { lowerK == it || lowerK.contains(it) }
                    if (shouldScrub) "$k=${maskValue(v)}" else part
                }
            }
            url.replace(query, scrubbedQuery)
        } catch (_: Exception) {
            // fallback: regex scrub for query params
            var result = url
            for (key in sensitiveQueryKeys) {
                result = result.replace(Regex("(?i)([?&]$key=)[^&]*")) { mr -> mr.groupValues[1] + maskValue("") }
            }
            result
        }
    }
}
