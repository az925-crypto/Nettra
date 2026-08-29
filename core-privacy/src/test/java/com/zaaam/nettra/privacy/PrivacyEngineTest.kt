package com.zaaam.nettra.privacy

import org.junit.Assert.*
import org.junit.Test

class PrivacyEngineTest {
    private val engine = PrivacyEngine()

    @Test fun blocksTrackerThirdParty() {
        assertTrue(engine.shouldBlock("https://doubleclick.net/pixel", "example.com"))
        assertTrue(engine.shouldBlock("https://sub.google-analytics.com/collect", "example.com"))
    }
    @Test fun doesNotBlockFirstParty() {
        assertFalse(engine.shouldBlock("https://example.com/api", "example.com"))
    }
    @Test fun doesNotBlockNonTracker() {
        assertFalse(engine.shouldBlock("https://cdn.example.com/app.js", "example.com"))
    }
    @Test fun upgradesHttp() {
        assertEquals("https://example.com/path", engine.shouldUpgradeToHttps("http://example.com/path"))
        assertNull(engine.shouldUpgradeToHttps("https://example.com"))
    }
    @Test fun isTrackerHost() {
        assertTrue(engine.isTrackerHost("doubleclick.net"))
        assertTrue(engine.isTrackerHost("sub.doubleclick.net"))
        assertFalse(engine.isTrackerHost("example.com"))
    }
}
