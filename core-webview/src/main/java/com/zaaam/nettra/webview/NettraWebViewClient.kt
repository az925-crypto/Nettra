package com.zaaam.nettra.webview

import android.graphics.Bitmap
import android.net.Uri
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

    @Volatile private var currentPageHost: String? = null

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // UI thread — cache host untuk first-party check di background thread
        currentPageHost = try { url?.let { Uri.parse(it).host?.lowercase() } } catch (_: Exception) { null }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        // Do not block main-frame document — prevents blank page on first-party navigation
        if (request?.isForMainFrame == true) return null
        // 1. Tracker blocking with first-party allow — JANGAN panggil view.getUrl() di background thread!
        // Pakai currentPageHost cache dari onPageStarted (UI thread) vs request.url.host
        val requestHost = try { request.url.host?.lowercase() } catch (_: Exception) { null } ?: return null
        // Jika request host == page host → first-party, jangan blok
        if (currentPageHost != null && requestHost == currentPageHost) return null
        // Fallback: jika request url string sama dengan page host string (tanpa view call) — sudah tercover di atas
        if (trackerBlocker?.shouldBlock(url, null) == true) {
            // Double-check first-party via host equality (sudah di atas), tapi untuk suffix match tetap perlu host check
            // shouldBlock dengan pageUrl null akan hanya suffix-match; kita sudah allow host equality di atas
            android.util.Log.d("NettraWebViewClient", "BLOCKED $url host=$requestHost pageHost=$currentPageHost")
            onTrackerBlocked(url)
            // Return empty response to block — request never leaves device for tracker
            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
        }
        // Untuk blocklist yang butuh pageUrl, kita reconstruct pageUrl dari currentPageHost jika ada
        // Tapi karena kita sudah allow host equality, cukup pakai shouldBlock tanpa pageUrl untuk sekarang
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
