package com.zaaam.nettra.inspector

import org.junit.Assert.*
import org.junit.Test

class NetworkInspectorTest {
    @Test fun recordAndRetrieve() {
        val insp = NetworkInspector()
        insp.recordRequest("tab1", "https://example.com/api", "GET")
        assertEquals(1, insp.getLog("tab1").size)
    }
    @Test fun maxPerTabEvictsOldest() {
        val insp = NetworkInspector()
        repeat(305) { insp.recordRequest("tab1", "https://example.com/$it") }
        assertEquals(300, insp.getLog("tab1").size)
    }
    @Test fun clearsPerTab() {
        val insp = NetworkInspector()
        insp.recordRequest("tab1", "https://a.com")
        insp.clear("tab1")
        assertEquals(0, insp.getLog("tab1").size)
    }
    @Test fun truncatesLargeBody() {
        val insp = NetworkInspector()
        val req = insp.recordRequest("tab1", "https://example.com/json")
        val big = "x".repeat(200*1024)
        insp.updateResponse("tab1", req.id, status = 200, body = big)
        val after = insp.getLog("tab1").first()
        assertTrue((after.bodyPreview?.length ?: 0) < big.length)
        assertTrue(after.bodyPreview!!.contains("truncated"))
    }
    @Test fun inMemoryNotPersistedSummary() {
        val insp = NetworkInspector()
        insp.recordRequest("tab1", "https://a.com")
        insp.recordRequest("tab1", "https://b.com", blocked = true)
        val s = insp.summary("tab1")
        assertEquals(2, s.totalRequests)
        assertEquals(1, s.blocked)
    }
}
