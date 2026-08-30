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

    @Test fun domainWithPortIsUrl() {
        assertTrue(SearchRouter.isValidUrl("example.com:8080"))
        assertTrue(SearchRouter.isValidUrl("example.com:3000"))
        assertTrue(SearchRouter.isValidUrl("sub.domain.co.id:8080"))
        assertEquals("https://example.com:8080", SearchRouter.resolve("example.com:8080"))
    }

    @Test fun domainWithPortAndPathIsUrl() {
        assertTrue(SearchRouter.isValidUrl("example.com:8080/path"))
        assertTrue(SearchRouter.isValidUrl("example.com:8080/path?q=1"))
        assertEquals("https://example.com:8080/path", SearchRouter.resolve("example.com:8080/path"))
    }

    @Test fun localhostWithPortVariants() {
        assertTrue(SearchRouter.isValidUrl("localhost"))
        assertTrue(SearchRouter.isValidUrl("localhost:3000"))
        assertTrue(SearchRouter.isValidUrl("localhost:8080/path"))
        assertTrue(SearchRouter.isValidUrl("localhost:80"))
        assertFalse(SearchRouter.isValidUrl("localhost:99999"))
        assertFalse(SearchRouter.isValidUrl("localhost:0"))
    }

    @Test fun invalidDomainLabelsAreNotUrl() {
        assertFalse(SearchRouter.isValidUrl("-.com"))
        assertFalse(SearchRouter.isValidUrl("-example.com"))
        assertFalse(SearchRouter.isValidUrl("example-.com"))
        assertFalse(SearchRouter.isValidUrl("example..com"))
        assertFalse(SearchRouter.isValidUrl(".example.com"))
        assertFalse(SearchRouter.isValidUrl("example.com."))
        assertFalse(SearchRouter.isValidUrl("exa..mple.com"))
    }

    @Test fun invalidPortIsNotUrl() {
        assertFalse(SearchRouter.isValidUrl("example.com:99999"))
        assertFalse(SearchRouter.isValidUrl("example.com:0"))
        assertFalse(SearchRouter.isValidUrl("example.com:abc"))
        assertFalse(SearchRouter.isValidUrl("example.com:"))
    }

    @Test fun buildSearchUrlEncodes() {
        val url = SearchRouter.buildSearchUrl("hello world")
        assertTrue(url.contains("hello+world") || url.contains("hello%20world"))
    }
}
