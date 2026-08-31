package com.zaaam.nettra.privacy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * FR-4: Tracker Blocking pakai Data DuckDuckGo — P0
 * Matches request URL against bundled blocklist (web) at shouldInterceptRequest.
 * 02_ARCHITECTURE.md: core-privacy operates at shouldInterceptRequest BEFORE request leaves.
 */
class TrackerBlocker(
    private val blocklist: BlocklistSnapshot
) {
    fun shouldBlock(requestUrl: String, pageUrl: String? = null): Boolean {
        val host = try { java.net.URL(requestUrl).host.lowercase() } catch (_: Exception) { return false }
        // First-party allow: if request host == page host, do not block top-level navigation
        if (pageUrl != null) {
            val pageHost = try { java.net.URL(pageUrl).host.lowercase() } catch (_: Exception) { null }
            if (pageHost != null && host == pageHost) return false
        }
        // We do simple suffix match for tracker domains (third-party only)
        val blocked = blocklist.trackers.any { tracker ->
            host == tracker.domain || host.endsWith(".${tracker.domain}")
        }
        if (blocked) android.util.Log.d("TrackerBlocker", "BLOCKED $requestUrl host=$host")
        return blocked
    }

    val version: String get() = blocklist.version
    val generatedAt: String get() = blocklist.generatedAt

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(raw: String): TrackerBlocker {
            val snapshot = json.decodeFromString<BlocklistSnapshot>(raw)
            return TrackerBlocker(snapshot)
        }
    }
}

@Serializable
data class BlocklistSnapshot(
    val version: String,
    val generatedAt: String,
    val trackers: List<TrackerEntry>
)

@Serializable
data class TrackerEntry(
    val domain: String,
    val category: String = "unknown"
)
