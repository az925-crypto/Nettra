package com.zaaam.nettra.webview

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.zaaam.nettra.inspector.NetworkInspector
import com.zaaam.nettra.privacy.PrivacyEngine
import com.zaaam.nettra.tabs.TabManager
import java.io.ByteArrayInputStream

class NetTraWebViewClient(
    private val tabId: String,
    private val tabManager: TabManager,
    private val privacyEngine: PrivacyEngine,
    private val inspector: NetworkInspector,
    private val onPageInfo: (url: String, title: String) -> Unit = { _, _ -> },
    private val onBlockedCount: (Int) -> Unit = {}
) : WebViewClient() {

    private val lock = Any()
    @Volatile private var pageHost: String? = null
    private var blockedInPage = 0

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url != null) {
            synchronized(lock) {
                pageHost = try { android.net.Uri.parse(url).host?.lowercase() } catch (_: Exception) { null }
                blockedInPage = 0
            }
            tabManager.onPageStarted(tabId)
            tabManager.updateTabUrl(tabId, url, view?.title ?: url)
            onPageInfo(url, view?.title ?: url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) onPageInfo(url, view?.title ?: url)
        val count: Int
        synchronized(lock) { count = blockedInPage }
        onBlockedCount(count)
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        if (request == null) return null
        val url = request.url.toString()
        val lowerUrl = url.trim().lowercase()
        // handle ws/wss: do not intercept
        if (lowerUrl.startsWith("ws://") || lowerUrl.startsWith("wss://")) return null
        val method = request.method ?: "GET"
        // Only log/block if inspector enabled for this tab
        val enabled = tabManager.tabs.value.find { it.entity.id == tabId }?.inspectorEnabled ?: false
        // canonicalize pageHost snapshot
        val hostSnapshot: String?
        synchronized(lock) { hostSnapshot = pageHost }
        // Check tracker block - always block regardless of inspector toggle (FR-12)
        val shouldBlock = privacyEngine.shouldBlock(url, hostSnapshot)
        if (shouldBlock) {
            synchronized(lock) { blockedInPage++ }
            if (enabled) {
                inspector.markBlocked(tabId, url, "Blocked by PrivacyEngine")
            }
            return WebResourceResponse("text/plain", "utf-8", 204, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        }

        if (enabled) {
            val headers = request.requestHeaders ?: emptyMap()
            inspector.recordRequest(tabId, url, method, headers)
        }

        // MVP Opsi A: return null to let WebView load normally (no re-fetch, no body capture)
        // Phase 2 would use local proxy for full body
        return null
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // HTTPS-first upgrade
        val url = request?.url.toString()
        val trimmed = url.trim()
        if (trimmed.lowercase().startsWith("http://")) {
            val upgraded = privacyEngine.shouldUpgradeToHttps(trimmed)
            if (upgraded != null) {
                view?.loadUrl(upgraded)
                return true
            }
        }
        return false
    }
}
