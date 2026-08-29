package com.zaaam.nettra.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.zaaam.nettra.privacy.TrackerBlocker
import java.io.ByteArrayInputStream

/**
 * Core-webview wrapper: delegates tracker blocking to core-privacy at shouldInterceptRequest
 * and HTTPS-first upgrade. 02_ARCHITECTURE.md data flow.
 */
class NettraWebViewClient(
    private val trackerBlocker: TrackerBlocker?,
    private val onTrackerBlocked: (String) -> Unit = {},
    private val onHttpsUpgrade: (String) -> Unit = {}
) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        // 1. Tracker blocking
        if (trackerBlocker?.shouldBlock(url) == true) {
            onTrackerBlocked(url)
            // Return empty response to block — request never leaves device for tracker
            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
        }
        return null // let WebView handle normally
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        // HTTPS-First: if http and https version exists, upgrade
        if (url.startsWith("http://")) {
            val https = url.replaceFirst("http://", "https://")
            // In real impl, check if https is reachable before upgrading; for MVP we upgrade directly
            // Caller can intercept via callback for warning UI
            if (https != url) onHttpsUpgrade(https)
        }
        return false
    }
}
