package com.zaaam.nettra.browserui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.nettra.privacy.BlocklistSnapshot
import com.zaaam.nettra.privacy.TrackerBlocker
import com.zaaam.nettra.privacy.TrackerEntry
import com.zaaam.nettra.search.SearchRouter
import com.zaaam.nettra.tabs.FireWiper
import com.zaaam.nettra.tabs.HistoryDao
import com.zaaam.nettra.tabs.TabDao
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
    var blockedTotal by mutableStateOf(1284)
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
    }

    fun closeTab(id: Long) {
        if (tabs.size == 1) return
        val wasActive = activeId == id
        tabs = tabs.filter { it.id != id }
        if (wasActive) {
            activeId = tabs.last().id
            addressInput = activeTab.let { if (it.url.startsWith("https://duckduckgo.com")) it.query else it.url }
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
                newTab = cur.copy(url = url, title = url, type = "http", secure = false, grade = "C", blocked = 0)
            } else {
                val isShop = url.contains("shop")
                val isNews = url.contains("news") || url.contains("berita")
                val blocked = when { isShop -> 9; isNews -> 7; url.contains("duckduckgo") -> 2; else -> 3 }
                val grade = if (blocked > 6) "B" else "A"
                blockedTotal += blocked
                newTab = cur.copy(url = url, title = extractHost(url), type = "site", blocked = blocked, grade = grade, secure = true)
            }
        } else {
            val ddg = SearchRouter.buildSearchUrl(input)
            newTab = cur.copy(url = ddg, query = input, title = "\"$input\" — DuckDuckGo", type = "results", blocked = 2, grade = "A", secure = true)
        }
        tabs = tabs.toMutableList().also { it[idx] = newTab }
        addressInput = if (newTab.type == "results") newTab.query else newTab.url
        showPrivacy = false
    }

    fun upgradeToHttps() {
        val cur = activeTab
        if (cur.url.startsWith("http://", true)) {
            val https = cur.url.replaceFirst("http://", "https://", true)
            val idx = tabs.indexOfFirst { it.id == activeId }
            tabs = tabs.toMutableList().also { it[idx] = cur.copy(url = https, title = extractHost(https), type = "site", secure = true, grade = "A", blocked = 1) }
            addressInput = https
        }
    }

    fun onTrackerBlocked() {
        val idx = tabs.indexOfFirst { it.id == activeId }
        if (idx == -1) return
        val cur = tabs[idx]
        tabs = tabs.toMutableList().also { it[idx] = cur.copy(blocked = cur.blocked + 1) }
        blockedTotal += 1
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
