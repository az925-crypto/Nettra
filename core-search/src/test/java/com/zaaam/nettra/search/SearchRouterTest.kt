package com.zaaam.nettra.search

import org.junit.Assert.*
import org.junit.Test

class SearchRouterTest {

    @Test fun queryGoesToDuckDuckGo() {
        val url = SearchRouter.resolve("resep nasi goreng")
        assertTrue(url.startsWith("https://duckduckgo.com/?q="))
        assertTrue(url.contains("resep"))
        assertFalse(url.contains("google"))
    }

    @Test fun validUrlNotTreatedAsQuery() {
        assertTrue(SearchRouter.isValidUrl("https://example.com"))
        assertTrue(SearchRouter.isValidUrl("example.com"))
        assertTrue(SearchRouter.isValidUrl("sub.domain.co.id/path?q=1"))
        assertFalse(SearchRouter.isValidUrl("resep nasi goreng"))
        assertEquals("https://example.com", SearchRouter.resolve("example.com"))
    }

    @Test fun localhostIsUrl() {
        assertTrue(SearchRouter.isValidUrl("localhost:8000"))
    }

    @Test fun buildSearchUrlEncodes() {
        val url = SearchRouter.buildSearchUrl("hello world")
        assertTrue(url.contains("hello+world") || url.contains("hello%20world"))
    }
}
