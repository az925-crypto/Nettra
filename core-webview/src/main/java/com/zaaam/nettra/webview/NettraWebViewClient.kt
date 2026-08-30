package com.zaaam.nettra.webview

import android.net.http.SslError
import android.webkit.SslErrorHandler
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
        // Do not block main-frame document — prevents blank page on first-party navigation
        if (request?.isForMainFrame == true) return null
        if (view?.url != null && url == view.url) return null
        // 1. Tracker blocking with first-party allow (compare request host vs page host)
        val pageUrl = view?.url
        if (trackerBlocker?.shouldBlock(url, pageUrl) == true) {
            onTrackerBlocked(url)
            // Return empty response to block — request never leaves device for tracker
            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
        }
        return null // let WebView handle normally
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        // HTTPS-First enforce: upgrade http to https and block cleartext (case-insensitive)
        if (url.startsWith("http://", ignoreCase = true)) {
            val https = url.replaceFirst("http://", "https://", ignoreCase = true)
            if (https != url) {
                onHttpsUpgrade(https)
                view?.loadUrl(https)
                return true
            }
        }
        return false
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        // FR-2 SSL fix: jangan proceed diam-diam, batalkan dan biarkan WebView tampilkan error page sistem
        // Ini mencegah ERR_SSL_VERSION_OR_CIPHER_MISMATCH blank tanpa info
        handler?.cancel()
    }
}
