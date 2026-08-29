package com.zaaam.nettra.tabs

import com.zaaam.nettra.inspector.NetworkInspector
import com.zaaam.nettra.tabs.model.TabEntity
import java.io.Closeable
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TabState(
    val entity: TabEntity,
    val inspectorEnabled: Boolean = false,
    val preserveLog: Boolean = false,
    val throttling: String = "OFF", // OFF, SLOW_3G, FAST_3G, OFFLINE
    val fingerprintLevel: String = "Balanced"
)

class TabManager(
    private val inspector: NetworkInspector? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : Closeable {
    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId

    private val maxConcurrentCaptureTabs = 5
    private val mutex = Mutex()

    fun createTab(url: String = "about:blank", isPrivate: Boolean = false): String {
        val id = UUID.randomUUID().toString()
        val entity = TabEntity(id = id, url = url, title = if (url == "about:blank") "Tab baru" else url, isPrivate = isPrivate)
        _tabs.update { it + TabState(entity) }
        _selectedId.value = id
        return id
    }

    fun selectTab(id: String) { validateSelect(id) }

    fun closeTab(id: String) {
        inspector?.clear(id)
        inspector?.clearConsole(id)
        inspector?.removeTabFlows(id)
        _tabs.update { it.filterNot { s -> s.entity.id == id } }
        if (_selectedId.value == id) _selectedId.value = _tabs.value.firstOrNull()?.entity?.id
        if (_tabs.value.isEmpty()) createTab()
    }

    fun closePrivateTabs() {
        val privateIds = _tabs.value.filter { it.entity.isPrivate }.map { it.entity.id }
        privateIds.forEach {
            inspector?.clear(it)
            inspector?.clearConsole(it)
            inspector?.removeTabFlows(it)
        }
        _tabs.update { it.filterNot { s -> s.entity.isPrivate } }
        if (_selectedId.value in privateIds) {
            _selectedId.value = _tabs.value.firstOrNull { !it.entity.isPrivate }?.entity?.id ?: _tabs.value.firstOrNull()?.entity?.id
        }
        if (_tabs.value.isEmpty()) createTab()
        // coordinator (MainActivity) must also clear Cookies/WebStorage/WebView cache
    }

    fun updateTabUrl(id: String, url: String, title: String) {
        _tabs.update { list -> list.map { if (it.entity.id == id) it.copy(entity = it.entity.copy(url = url, title = title, lastActiveAt = System.currentTimeMillis())) else it } }
    }

    fun setInspectorEnabled(id: String, enabled: Boolean): Boolean {
        var result = false
        _tabs.update { current ->
            val found = current.find { it.entity.id == id } ?: return@update current
            if (found.inspectorEnabled == enabled) { result = true; return@update current }
            val enabledCount = current.count { it.inspectorEnabled }
            if (enabled && enabledCount >= maxConcurrentCaptureTabs) { result = false; return@update current }
            result = true
            current.map { if (it.entity.id == id) it.copy(inspectorEnabled = enabled) else it }
        }
        return result
    }

    suspend fun setInspectorEnabledSuspend(id: String, enabled: Boolean): Boolean = mutex.withLock {
        setInspectorEnabled(id, enabled)
    }

    fun setPreserveLog(id: String, preserve: Boolean) {
        _tabs.update { list -> list.map { if (it.entity.id == id) it.copy(preserveLog = preserve) else it } }
    }

    fun setThrottling(id: String, profile: String) {
        _tabs.update { list -> list.map { if (it.entity.id == id) it.copy(throttling = profile) else it } }
    }

    fun setFingerprintLevel(id: String, level: String) {
        _tabs.update { list -> list.map { if (it.entity.id == id) it.copy(fingerprintLevel = level) else it } }
    }

    fun onPageStarted(id: String) {
        val tab = _tabs.value.find { it.entity.id == id } ?: return
        if (!tab.preserveLog) inspector?.clear(id)
    }

    fun validateSelect(id: String): Boolean {
        if (_tabs.value.none { it.entity.id == id }) return false
        _selectedId.value = id
        return true
    }

    fun currentTab(): TabState? = _tabs.value.find { it.entity.id == _selectedId.value }

    fun destroy() { scope.cancel() }
    override fun close() { scope.cancel() }

    init { if (_tabs.value.isEmpty()) createTab() }
}
