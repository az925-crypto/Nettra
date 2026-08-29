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
    fun shouldBlock(requestUrl: String): Boolean {
        val host = try { java.net.URL(requestUrl).host.lowercase() } catch (_: Exception) { return false }
        // Allow first-party: exact host already checked as tracker list is third-party only.
        // We do simple suffix match for tracker domains.
        return blocklist.trackers.any { tracker ->
            host == tracker.domain || host.endsWith(".${tracker.domain}")
        }
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
