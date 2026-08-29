package com.zaaam.nettra.inspector

import com.zaaam.nettra.inspector.model.CapturedRequest
import com.zaaam.nettra.inspector.model.ResourceType
import com.zaaam.nettra.inspector.model.TimingInfo
import com.zaaam.nettra.inspector.model.classifyResource
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
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
        _logsFlow.computeIfAbsent(tabId) { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }

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
    private fun getOrCreateConsoleFlow(tabId: String) = _consoleFlow.computeIfAbsent(tabId) { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }
    fun getConsoleFlow(tabId: String): kotlinx.coroutines.flow.StateFlow<List<com.zaaam.nettra.inspector.model.ConsoleEntry>> = getOrCreateConsoleFlow(tabId)
    fun addConsoleLog(tabId: String, entry: com.zaaam.nettra.inspector.model.ConsoleEntry) {
        val list = consoleLogs.computeIfAbsent(tabId) { mutableListOf() }
        synchronized(list) { if (list.size >= 200) list.removeAt(0); list.add(entry) }
        getOrCreateConsoleFlow(tabId).value = synchronized(list) { list.toList() }
    }
    fun getConsoleLog(tabId: String): List<com.zaaam.nettra.inspector.model.ConsoleEntry> = consoleLogs[tabId]?.toList() ?: emptyList()
    fun clearConsole(tabId: String) {
        consoleLogs[tabId]?.let { list -> synchronized(list) { list.clear() } }
        _consoleFlow[tabId]?.value = emptyList()
    }
    fun removeTabFlows(tabId: String) {
        // Called from TabManager.closeTab to prevent leak of _logsFlow/_consoleFlow
        logs.remove(tabId)
        consoleLogs.remove(tabId)
        _logsFlow.remove(tabId)
        _consoleFlow.remove(tabId)
    }

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
            val preview = body?.let { str ->
                if (str.isEmpty()) str else truncateBody(str)
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

    // Efficient truncation using CharsetEncoder/ByteBuffer without allocating full byte array
    private fun truncateBody(str: String): String {
        // Hard limit check: if encoded size > 1MB -> "too large"
        // Use encoder with limited ByteBuffer to avoid OOM on huge strings (7GB)
        val hardEncoder = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        val cbHard = CharBuffer.wrap(str)
        val bbHard = ByteBuffer.allocate(maxBodyHardLimit + 1)
        val crHard = hardEncoder.encode(cbHard, bbHard, true)
        hardEncoder.flush(bbHard)
        // If overflow or still has remaining chars, size exceeds hard limit
        if (crHard.isOverflow || cbHard.hasRemaining() || bbHard.position() > maxBodyHardLimit) {
            return "too large"
        }
        // Fits within hard limit; check preview limit
        if (bbHard.position() <= maxBodyPreview) {
            return str
        }
        // Need preview truncation to 100KB
        val previewEncoder = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        val cbPrev = CharBuffer.wrap(str)
        val bbPrev = ByteBuffer.allocate(maxBodyPreview)
        previewEncoder.encode(cbPrev, bbPrev, true)
        previewEncoder.flush(bbPrev)
        // cbPrev.position() = number of chars that fit within maxBodyPreview bytes without splitting multi-byte
        val cut = cbPrev.position()
        val truncated = if (cut > 0) str.substring(0, cut) else ""
        return truncated + "… (truncated)"
    }

    fun markBlocked(tabId: String, url: String, reason: String) {
        recordRequest(tabId, url, blocked = true, blockedReason = reason)
    }

    private fun add(tabId: String, req: CapturedRequest) {
        val list = logs.computeIfAbsent(tabId) { mutableListOf() }
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
        // also clear console for consistency; use clear() not remove() to keep flows alive but empty
        consoleLogs[tabId]?.let { cList -> synchronized(cList) { cList.clear() } }
        _logsFlow[tabId]?.value = emptyList()
        _consoleFlow[tabId]?.value = emptyList()
        emit(tabId)
    }
    fun clearAll() {
        logs.forEach { (_, list) -> synchronized(list) { list.clear() } }
        consoleLogs.forEach { (_, list) -> synchronized(list) { list.clear() } }
        logs.clear()
        consoleLogs.clear()
        startTimes.clear()
        _logsFlow.forEach { (_, flow) -> flow.value = emptyList() }
        _consoleFlow.forEach { (_, flow) -> flow.value = emptyList() }
        _logsFlow.clear()
        _consoleFlow.clear()
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
