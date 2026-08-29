package com.zaaam.nettra.inspector.model

data class CapturedRequest(
    val id: String,
    val tabId: String,
    val url: String,
    val method: String = "GET",
    val status: Int? = null, // null = blocked or unknown in Opsi A
    val type: ResourceType = ResourceType.Other,
    val size: Long? = null,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val durationMs: Long? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String>? = null,
    val bodyPreview: String? = null, // truncated 100KB
    val timing: TimingInfo? = null,
    val blocked: Boolean = false,
    val blockedReason: String? = null,
    val originalRequestHeaders: Map<String, String>? = null,
    val originalResponseHeaders: Map<String, String>? = null
) {
    // backward compat alias for older code/tests expecting originalHeaders
    val originalHeaders: Map<String, String>? get() = originalRequestHeaders
}

data class TimingInfo(
    val queueing: Long? = null,
    val dns: Long? = null,
    val connecting: Long? = null,
    val ttfb: Long? = null,
    val download: Long? = null,
    val total: Long
)
