package com.zaaam.nettra.inspector

import org.junit.Assert.*
import org.junit.Test

class HeaderMaskingTest {
    @Test fun masksAuthorization() { assertTrue(HeaderMasking.shouldMask("Authorization")) }
    @Test fun masksCookie() { assertTrue(HeaderMasking.shouldMask("Cookie")); assertTrue(HeaderMasking.shouldMask("Set-Cookie")) }
    @Test fun masksApiKeyVariants() { assertTrue(HeaderMasking.shouldMask("X-Api-Key")); assertTrue(HeaderMasking.shouldMask("x-api-key")) }
    @Test fun doesNotMaskContentType() { assertFalse(HeaderMasking.shouldMask("Content-Type")) }
    @Test fun masksTokenPattern() { assertTrue(HeaderMasking.shouldMask("X-Auth-Token")) }
    @Test fun maskValue() { assertEquals("••••••••", HeaderMasking.maskValue("secret123")) }
    @Test fun maskMap() {
        val m = mapOf("Authorization" to "Bearer abc", "Content-Type" to "application/json")
        val masked = HeaderMasking.mask(m)
        assertEquals("••••••••", masked["Authorization"])
        assertEquals("application/json", masked["Content-Type"])
    }
}
