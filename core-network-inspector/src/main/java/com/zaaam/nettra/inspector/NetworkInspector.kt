package com.zaaam.nettra.inspector

import com.zaaam.nettra.inspector.model.CapturedRequest
import com.zaaam.nettra.inspector.model.ResourceType
import com.zaaam.nettra.inspector.model.TimingInfo
import com.zaaam.nettra.inspector.model.classifyResource
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NetworkInspector {
    // In-memory only, per tab, not persisted (NFR security)
    private val logs = ConcurrentHashMap<String, MutableList<CapturedRequest>>()
    private val startTimes = ConcurrentHashMap<String, Long>() // requestId -> start

    private val maxPerTab = 300
    private val maxBodyPreview = 100 * 1024 // 100KB
    private val maxBodyHardLimit = 1024 * 1024 // 1MB

    private val _logsFlow = ConcurrentHashMap<String, kotlinx.coroutines.flow.MutableStateFlow<List<CapturedRequest>>>()
    private fun getOrCreateFlow(tabId: String): kotlinx.coroutines.flow.MutableStateFlow<List<CapturedRequest>> =
        _logsFlow.getOrPut(tabId) { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }

    fun getLogFlow(tabId: String): kotlinx.coroutines.flow.StateFlow<List<CapturedRequest>> = getOrCreateFlow(tabId)

    private fun emit(tabId: String) {
        val list = logs[tabId]
        val flow = _logsFlow[tabId]
        if (flow != null) {
            flow.value = if (list != null) synchronized(list) { list.toList() } else emptyList()
        }
    }

    // Phase2: console logs per tab
    private val consoleLogs = ConcurrentHashMap<String, MutableList<com.zaaam.nettra.inspector.model.ConsoleEntry>>()
    private val _consoleFlow = ConcurrentHashMap<String, kotlinx.coroutines.flow.MutableStateFlow<List<com.zaaam.nettra.inspector.model.ConsoleEntry>>>()
    private fun getOrCreateConsoleFlow(tabId: String) = _consoleFlow.getOrPut(tabId) { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }
    fun getConsoleFlow(tabId: String): kotlinx.coroutines.flow.StateFlow<List<com.zaaam.nettra.inspector.model.ConsoleEntry>> = getOrCreateConsoleFlow(tabId)
    fun addConsoleLog(tabId: String, entry: com.zaaam.nettra.inspector.model.ConsoleEntry) {
        val list = consoleLogs.getOrPut(tabId) { mutableListOf() }
        synchronized(list) { if (list.size >= 200) list.removeAt(0); list.add(entry) }
        getOrCreateConsoleFlow(tabId).value = synchronized(list) { list.toList() }
    }
    fun getConsoleLog(tabId: String): List<com.zaaam.nettra.inspector.model.ConsoleEntry> = consoleLogs[tabId]?.toList() ?: emptyList()
    fun clearConsole(tabId: String) { consoleLogs[tabId]?.clear(); getOrCreateConsoleFlow(tabId).value = emptyList() }

    fun recordRequest(
        tabId: String,
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        contentType: String? = null,
        blocked: Boolean = false,
        blockedReason: String? = null
    ): CapturedRequest {
        val id = UUID.randomUUID().toString()
        val type = classifyResource(url, contentType)
        val now = System.currentTimeMillis()
        startTimes[id] = now
        val scrubbedUrl = HeaderMasking.scrubUrl(url)
        val maskedHeaders = HeaderMasking.mask(headers)
        val req = CapturedRequest(
            id = id, tabId = tabId, url = scrubbedUrl, method = method,
            type = type, startTime = now, requestHeaders = maskedHeaders,
            originalRequestHeaders = headers.toMap(),
            blocked = blocked, blockedReason = blockedReason,
            status = if (blocked) null else null,
            timing = TimingInfo(total = 0)
        )
        add(tabId, req)
        return req
    }

    fun updateResponse(
        tabId: String,
        requestId: String,
        status: Int? = null,
        responseHeaders: Map<String, String>? = null,
        body: String? = null,
        size: Long? = null
    ) {
        val list = logs[tabId] ?: return
        synchronized(list) {
            val idx = list.indexOfFirst { it.id == requestId }
            if (idx == -1) {
                startTimes.remove(requestId)
                return
            }
            val old = list[idx]
            val end = System.currentTimeMillis()
            val duration = end - old.startTime
            val preview = body?.let {
                val bytes = it.toByteArray(Charsets.UTF_8)
                when {
                    bytes.size > maxBodyHardLimit -> "too large"
                    bytes.size > maxBodyPreview -> {
                        // truncate by bytes correctly, avoid splitting multi-byte: take substring that fits byte limit
                        var truncated = it
                        // binary search for safe truncation point
                        var low = 0
                        var high = it.length
                        var best = maxBodyPreview
                        // simple: take maxBodyPreview chars as approximate, then adjust
                        truncated = it.take(maxBodyPreview)
                        while (truncated.toByteArray(Charsets.UTF_8).size > maxBodyPreview && truncated.isNotEmpty()) {
                            truncated = truncated.dropLast(1)
                        }
                        truncated + "… (truncated)"
                    }
                    else -> it
                }
            }
            val maskedResponseHeaders = responseHeaders?.let { HeaderMasking.mask(it) }
            val origRespHeaders = responseHeaders?.toMap()
            list[idx] = old.copy(
                status = status ?: old.status,
                responseHeaders = maskedResponseHeaders ?: old.responseHeaders,
                originalResponseHeaders = origRespHeaders ?: old.originalResponseHeaders,
                bodyPreview = preview ?: old.bodyPreview,
                size = size ?: old.size,
                endTime = end,
                durationMs = duration,
                timing = TimingInfo(total = duration)
            )
        }
        startTimes.remove(requestId)
        emit(tabId)
    }

    fun markBlocked(tabId: String, url: String, reason: String) {
        recordRequest(tabId, url, blocked = true, blockedReason = reason)
    }

    private fun add(tabId: String, req: CapturedRequest) {
        val list = logs.getOrPut(tabId) { mutableListOf() }
        synchronized(list) {
            if (list.size >= maxPerTab) {
                val removed = list.removeAt(0)
                startTimes.remove(removed.id)
            }
            list.add(req)
        }
        emit(tabId)
    }

    fun getLog(tabId: String): List<CapturedRequest> {
        val list = logs[tabId] ?: return emptyList()
        synchronized(list) { return list.toList() }
    }
    fun clear(tabId: String) {
        val list = logs[tabId]
        if (list != null) {
            val ids: List<String>
            synchronized(list) {
                ids = list.map { it.id }
                list.clear()
            }
            ids.forEach { startTimes.remove(it) }
        }
        emit(tabId)
    }
    fun clearAll() {
        logs.forEach { (_, list) -> synchronized(list) { list.clear() } }
        logs.clear()
        startTimes.clear()
        _logsFlow.forEach { (_, flow) -> flow.value = emptyList() }
    }

    fun summary(tabId: String): Summary {
        val list = getLog(tabId)
        val totalSize = list.mapNotNull { it.size }.sum()
        val totalTime = list.mapNotNull { it.durationMs }.maxOrNull() ?: 0L
        val blocked = list.count { it.blocked }
        return Summary(totalRequests = list.size, transferred = totalSize, loadTimeMs = totalTime, blocked = blocked)
    }

    data class Summary(val totalRequests: Int, val transferred: Long, val loadTimeMs: Long, val blocked: Int)
}
