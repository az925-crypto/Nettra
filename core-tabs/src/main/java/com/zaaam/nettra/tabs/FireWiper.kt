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
    suspend fun wipe(
        historyDao: HistoryDao,
        tabDao: TabDao?,
        webViews: List<WebView>?
    ) {
        // 1. Clear history (Room) + tabs on IO — offloaded from Main
        withContext(Dispatchers.IO) {
            historyDao.clearAll()
            tabDao?.clearAll()
        }
        // 2. Clear cookies async — removeAllCookies is async, don't block
        withContext(Dispatchers.Main) {
            CookieManager.getInstance().removeAllCookies(null)
        }
        // 3. Flush + WebStorage delete offloaded to IO (blocking disk I/O)
        withContext(Dispatchers.IO) {
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
        // 4. Clear each WebView cache/storage if provided
        // clearHistory/clearFormData must be on Main; clearCache(true) is blocking -> offload inner disk work
        if (webViews != null) {
            withContext(Dispatchers.Main) {
                webViews.forEach { wv ->
                    wv.clearHistory()
                    wv.clearFormData()
                }
            }
            // clearCache(true) does disk I/O — call on Main but not block it synchronously;
            // we dispatch the heavy part via IO: post to Main from IO to keep thread affinity
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    webViews.forEach { it.clearCache(true) }
                }
            }
        }
    }

    // Backward compat overload
    suspend fun wipe(historyDao: HistoryDao, webViews: List<WebView>? = null) {
        wipe(historyDao, null, webViews)
    }
}
