package com.zaaam.nettra.inspector

import com.zaaam.nettra.inspector.model.CapturedRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class HarCreator(val name: String = "NetTra", val version: String = "1.0.0")
@Serializable data class HarLog(val version: String = "1.2", val creator: HarCreator = HarCreator(), val entries: List<HarEntry>)
@Serializable data class Har(val log: HarLog)
@Serializable data class HarEntry(val startedDateTime: String, val time: Long, val request: HarRequest, val response: HarResponse)
@Serializable data class HarRequest(val method: String, val url: String, val headers: List<HarHeader>)
@Serializable data class HarResponse(val status: Int, val content: HarContent, val headers: List<HarHeader>)
@Serializable data class HarHeader(val name: String, val value: String)
@Serializable data class HarContent(val size: Long, val mimeType: String, val text: String?)

object HarExporter {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private fun resolveMime(req: CapturedRequest): String {
        // Try responseHeaders content-type case-insensitive, then requestHeaders, else original headers
        val candidates = listOfNotNull(req.responseHeaders, req.originalResponseHeaders, req.requestHeaders, req.originalRequestHeaders)
        for (map in candidates) {
            val entry = map.entries.firstOrNull { it.key.equals("content-type", ignoreCase = true) }
            if (entry != null && entry.value.isNotBlank()) {
                return entry.value.split(";")[0].trim().ifBlank { "text/plain" }
            }
        }
        // fallback: infer from type? use text/plain as spec says
        return "text/plain"
    }
    private fun maskBody(body: String?): String? {
        if (body == null) return null
        var masked = body
        val sensitive = listOf("password","token","secret","api_key","apikey","api-key","auth","authorization","access_token","refresh_token")
        for (key in sensitive) {
            masked = masked.replace(Regex("(?i)\"$key\"\\s*:\\s*\"[^\"]*\""), "\"$key\":\"***\"")
            masked = masked.replace(Regex("(?i)$key\\s*=\\s*[^&\\s\"]+"), "$key=***")
        }
        return masked
    }
    fun export(log: List<CapturedRequest>): String {
        val entries = log.map { req ->
            HarEntry(
                startedDateTime = java.time.Instant.ofEpochMilli(req.startTime).toString(),
                time = req.durationMs ?: 0,
                request = HarRequest(req.method, req.url, req.requestHeaders.map { HarHeader(it.key, it.value) }),
                response = HarResponse(req.status ?: 0, HarContent(req.size ?: 0, resolveMime(req), maskBody(req.bodyPreview)), (req.responseHeaders ?: emptyMap()).map { HarHeader(it.key, it.value) })
            )
        }
        return json.encodeToString(Har(HarLog(entries = entries)))
    }
}
