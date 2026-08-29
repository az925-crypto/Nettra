package com.zaaam.nettra.tabs

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FR-6: Fire Button — P0
 * Must clear at storage level (CookieManager, WebStorage), not just UI.
 * Bookmark NOT deleted — 04_DEFINITION_OF_DONE.md
 * 02_ARCHITECTURE.md: core-tabs triggers wipe via core-webview storage.
 */
object FireWiper {
    suspend fun wipe(historyDao: HistoryDao, webViews: List<WebView>? = null) = withContext(Dispatchers.Main) {
        // 1. Clear history (Room) — bookmark stays
        withContext(Dispatchers.IO) { historyDao.clearAll() }
        // 2. Clear cookies
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        // 3. Clear WebStorage (localStorage, etc.)
        WebStorage.getInstance().deleteAllData()
        // 4. Clear each WebView cache/storage if provided
        webViews?.forEach { wv ->
            wv.clearCache(true)
            wv.clearHistory()
            wv.clearFormData()
        }
    }
}
