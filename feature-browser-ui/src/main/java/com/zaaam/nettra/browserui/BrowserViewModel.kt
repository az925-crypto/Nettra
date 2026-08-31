package com.zaaam.nettra.browserui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.nettra.privacy.BlocklistSnapshot
import com.zaaam.nettra.privacy.TrackerBlocker
import com.zaaam.nettra.privacy.TrackerEntry
import android.util.Log
import com.zaaam.nettra.privacy.PrivacyReport
import com.zaaam.nettra.search.SearchRouter
import com.zaaam.nettra.tabs.FireWiper
import com.zaaam.nettra.tabs.HistoryDao
import com.zaaam.nettra.tabs.TabDao
import com.zaaam.nettra.tabs.TabEntity
import kotlinx.coroutines.launch

data class TabState(
    val id: Long,
    val title: String,
    val url: String,
    val query: String = "",
    val type: String = "newtab", // newtab | results | site | http
    val blocked: Int = 0,
    val grade: String = "A",
    val secure: Boolean = true,
    val isPrivate: Boolean = false
)

class BrowserViewModel(
    private val historyDao: HistoryDao? = null,
    private val tabDao: TabDao? = null
) : ViewModel() {

    init {
        // Load persisted tabs if DB available — FR-3 persist, avoid race overwriting newTab
        if (tabDao != null) {
            viewModelScope.launch {
                try {
                    val persisted = tabDao.getAll()
                    if (persisted.isNotEmpty()) {
                        // Only overwrite if still initial state (no user tab yet) to avoid race
                        val isInitial = tabs.size == 1 && tabs[0].id == 1L && tabs[0].url.isEmpty() && tabs[0].title == "Tab baru"
                        if (isInitial) {
                            val mapped = persisted.reversed().map { e ->
                                TabState(id = e.id, title = e.title, url = e.url, query = e.query, type = e.type, blocked = e.blocked, grade = e.grade, secure = e.secure, isPrivate = e.isPrivate)
                            }
                            tabs = mapped
                            activeId = mapped.first().id
                            nextId = (mapped.maxOfOrNull { it.id } ?: 0) + 1
                            blockedTotal = mapped.sumOf { it.blocked }
                            addressInput = activeTab.let { if (it.url.startsWith("https://duckduckgo.com")) it.query else it.url }
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    // Blocklist — bundled snapshot v2026.08.21 (fallback hardcoded kalau assets belum load)
    val trackerBlocker: TrackerBlocker = TrackerBlocker(
        BlocklistSnapshot(
            version = "v2026.08.21",
            generatedAt = "2026-08-21T00:00:00Z",
            trackers = listOf(
                TrackerEntry("google-analytics.com", "analytics"),
                TrackerEntry("doubleclick.net", "ads"),
                TrackerEntry("facebook.net", "social"),
                TrackerEntry("facebook.com", "social"),
                TrackerEntry("googletagmanager.com", "analytics"),
                TrackerEntry("hotjar.com", "analytics"),
                TrackerEntry("adservice.google.com", "ads"),
                TrackerEntry("googlesyndication.com", "ads")
            )
        )
    )

    var tabs by mutableStateOf(listOf(TabState(id = 1, title = "Tab baru", url = "")))
        private set
    var activeId by mutableStateOf(1L)
        private set
    private var nextId = 2L
    var addressInput by mutableStateOf("")
        private set
    var blockedTotal by mutableStateOf(0)
        private set
    var showPrivacy by mutableStateOf(false)
        private set
    var showFireDialog by mutableStateOf(false)
        private set
    var showTabSwitcher by mutableStateOf(false)
        private set
    var showMenu by mutableStateOf(false)
        private set

    val activeTab: TabState get() = tabs.find { it.id == activeId } ?: tabs.first()

    fun onAddressChange(v: String) { addressInput = v }

    fun switchTab(id: Long) {
        activeId = id
        addressInput = activeTab.let { if (it.url.startsWith("https://duckduckgo.com")) it.query else it.url }
        showTabSwitcher = false
    }

    fun newTab(private: Boolean = false) {
        val t = TabState(id = nextId++, title = if (private) "Private — Tab baru" else "Tab baru", url = "", isPrivate = private)
        tabs = tabs + t
        activeId = t.id
        addressInput = ""
        // Persist if DB available
        tabDao?.let { dao ->
            viewModelScope.launch {
                try { dao.insert(TabEntity(id = t.id, url = t.url, title = t.title, isPrivate = t.isPrivate, type = t.type, query = t.query, blocked = t.blocked, grade = t.grade, secure = t.secure)) } catch (_: Exception) { }
            }
        }
    }

    fun closeTab(id: Long) {
        if (tabs.size == 1) return
        val closed = tabs.find { it.id == id } ?: return
        val wasActive = activeId == id
        tabs = tabs.filter { it.id != id }
        blockedTotal -= closed.blocked
        if (blockedTotal < 0) blockedTotal = 0
        if (wasActive) {
            activeId = tabs.last().id
            addressInput = activeTab.let { if (it.url.startsWith("https://duckduckgo.com")) it.query else it.url }
        }
        tabDao?.let { dao ->
            viewModelScope.launch {
                try { dao.deleteById(id) } catch (_: Exception) { }
            }
        }
    }

    fun navigate(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty()) return
        val isUrl = SearchRouter.isValidUrl(input)
        val idx = tabs.indexOfFirst { it.id == activeId }
        if (idx == -1) return
        val cur = tabs[idx]
        val newTab: TabState
        if (isUrl) {
            var url = input
            if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) url = "https://$url"
            // HTTPS-First: if http, keep as http type to show warning (FR-5)
            if (url.startsWith("http://", true)) {
                val blocked = 0
                val secure = false
                val grade = PrivacyReport.gradeFor(blocked, secure)
                newTab = cur.copy(url = url, title = url, type = "http", secure = secure, grade = grade, blocked = blocked)
                blockedTotal = blockedTotal - cur.blocked + blocked
            } else {
                val blocked = 0
                val secure = true
                val grade = PrivacyReport.gradeFor(blocked, secure)
                blockedTotal = blockedTotal - cur.blocked + blocked
                newTab = cur.copy(url = url, title = extractHost(url), type = "site", blocked = blocked, grade = grade, secure = secure)
            }
        } else {
            val ddg = SearchRouter.buildSearchUrl(input)
            val blocked = 0
            val secure = true
            val grade = PrivacyReport.gradeFor(blocked, secure)
            newTab = cur.copy(url = ddg, query = input, title = "\"$input\" — DuckDuckGo", type = "results", blocked = blocked, grade = grade, secure = secure)
            blockedTotal = blockedTotal - cur.blocked + blocked
        }
        tabs = tabs.toMutableList().also { it[idx] = newTab }
        addressInput = if (newTab.type == "results") newTab.query else newTab.url
        showPrivacy = false
        // Persist navigate state
        tabDao?.let { dao ->
            viewModelScope.launch {
                try {
                    dao.updateTab(newTab.id, newTab.url, newTab.title, newTab.type, newTab.query, newTab.blocked, newTab.grade, newTab.secure)
                    // If update affected 0 rows (tab not yet persisted), insert
                    val existing = dao.getAll().find { it.id == newTab.id }
                    if (existing == null) {
                        dao.insert(TabEntity(id = newTab.id, url = newTab.url, title = newTab.title, isPrivate = newTab.isPrivate, type = newTab.type, query = newTab.query, blocked = newTab.blocked, grade = newTab.grade, secure = newTab.secure))
                    }
                } catch (_: Exception) {
                    try { dao.insert(TabEntity(id = newTab.id, url = newTab.url, title = newTab.title, isPrivate = newTab.isPrivate, type = newTab.type, query = newTab.query, blocked = newTab.blocked, grade = newTab.grade, secure = newTab.secure)) } catch (_: Exception) { }
                }
            }
        }
        // History persist for FR-3 — skip private tabs (FR-8)
        if (!newTab.isPrivate) {
            historyDao?.let { dao ->
                viewModelScope.launch {
                    try {
                        val title = newTab.title.takeIf { it.isNotEmpty() } ?: newTab.url
                        if (newTab.url.isNotEmpty()) dao.insert(com.zaaam.nettra.tabs.HistoryEntity(url = newTab.url, title = title))
                    } catch (_: Exception) { }
                }
            }
        }
    }

    fun upgradeToHttps() {
        val cur = tabs.find { it.id == activeId } ?: return
        if (cur.url.startsWith("http://", true)) {
            val https = cur.url.replaceFirst("http://", "https://", true)
            val idx = tabs.indexOfFirst { it.id == activeId }
            if (idx == -1) return
            val blocked = 0
            val secure = true
            val grade = PrivacyReport.gradeFor(blocked, secure)
            val updated = cur.copy(url = https, title = extractHost(https), type = "site", secure = secure, grade = grade, blocked = blocked)
            blockedTotal = blockedTotal - cur.blocked + blocked
            tabs = tabs.toMutableList().also { it[idx] = updated }
            addressInput = https
            tabDao?.let { dao ->
                viewModelScope.launch {
                    try { dao.updateTab(updated.id, updated.url, updated.title, updated.type, updated.query, updated.blocked, updated.grade, updated.secure) } catch (_: Exception) { }
                }
            }
        }
    }

    fun onTrackerBlocked() {
        val idx = tabs.indexOfFirst { it.id == activeId }
        if (idx == -1) return
        val cur = tabs[idx]
        val blocked = cur.blocked + 1
        val grade = PrivacyReport.gradeFor(blocked, cur.secure)
        val updated = cur.copy(blocked = blocked, grade = grade)
        tabs = tabs.toMutableList().also { it[idx] = updated }
        blockedTotal += 1
        Log.d("Nettra", "Tracker blocked: ${cur.url} -> blocked=$blocked grade=$grade")
        tabDao?.let { dao ->
            viewModelScope.launch {
                try { dao.updateTab(updated.id, updated.url, updated.title, updated.type, updated.query, updated.blocked, updated.grade, updated.secure) } catch (_: Exception) { }
            }
        }
    }

    fun requestFire() { showFireDialog = true }
    fun dismissFire() { showFireDialog = false }
    fun doFire() {
        // FireWiper wipe at storage level if DAOs injected; fallback to in-memory only for preview/tests
        historyDao?.let { dao ->
            viewModelScope.launch {
                try {
                    FireWiper.wipe(dao, tabDao, null)
                } catch (_: Exception) { }
            }
        }
        // Also clear tabs DB if only tabDao present without historyDao
        if (historyDao == null && tabDao != null) {
            viewModelScope.launch {
                try { tabDao.clearAll() } catch (_: Exception) { }
            }
        }
        tabs = listOf(TabState(id = nextId++, title = "Tab baru", url = ""))
        activeId = tabs.first().id
        addressInput = ""
        blockedTotal = 0
        showFireDialog = false
        showPrivacy = false
        showTabSwitcher = false
        showMenu = false
    }

    fun togglePrivacy(v: Boolean? = null) { showPrivacy = v ?: !showPrivacy }
    fun toggleTabSwitcher(v: Boolean? = null) { showTabSwitcher = v ?: !showTabSwitcher }
    fun toggleMenu(v: Boolean? = null) { showMenu = v ?: !showMenu }

    private fun extractHost(url: String): String = try { java.net.URL(url).host } catch (_: Exception) { url }
}
