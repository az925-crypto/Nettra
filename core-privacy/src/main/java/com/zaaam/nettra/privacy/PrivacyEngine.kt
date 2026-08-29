package com.zaaam.nettra.privacy

import java.net.URI

class PrivacyEngine(
    private val blockList: Set<String> = defaultBlockList()
) {
    // FR-12: block third-party tracker
    // FR-13: third-party cookie blocked via CookiePolicy, not here
    // FR-14: HTTPS upgrade

    fun shouldBlock(requestUrl: String, pageHost: String?): Boolean {
        val host = try { URI(requestUrl).host?.lowercase() ?: return false } catch (_: Exception) { return false }
        if (pageHost == null) return false
        val pageHostLower = pageHost.lowercase()
        if (host == pageHostLower || host.endsWith("." + pageHostLower)) return false // first-party never blocked
        // simple suffix match
        return blockList.any { host == it || host.endsWith(".$it") }
    }

    fun isTrackerHost(host: String): Boolean =
        blockList.any { host.lowercase() == it || host.lowercase().endsWith(".$it") }

    fun shouldUpgradeToHttps(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.lowercase().startsWith("http://")) {
            val https = "https://" + trimmed.substring(7)
            return https
        }
        return null
    }

    companion object {
        fun defaultBlockList(): Set<String> = setOf(
            "doubleclick.net", "googletagmanager.com", "google-analytics.com",
            "facebook.net", "facebook.com", "connect.facebook.net",
            "googlesyndication.com", "adservice.google.com",
            "hotjar.com", "mixpanel.com", "segment.com", "amplitude.com",
            "tracker.example", "pixel.track", "ads.yahoo.com", "scorecardresearch.com",
            "criteo.net", "criteo.com", "outbrain.com", "taboola.com"
        )
    }
}
