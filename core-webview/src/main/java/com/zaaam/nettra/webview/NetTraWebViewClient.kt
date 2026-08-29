package com.zaaam.nettra.webview

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.zaaam.nettra.inspector.BodyCaptureHelper
import com.zaaam.nettra.inspector.NetworkInspector
import com.zaaam.nettra.privacy.PrivacyEngine
import com.zaaam.nettra.tabs.TabManager
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetTraWebViewClient(
    private val tabId: String,
    private val tabManager: TabManager,
    private val privacyEngine: PrivacyEngine,
    private val inspector: NetworkInspector,
    private val customBlocklist: Set<String> = emptySet(),
    private val onPageInfo: (url: String, title: String) -> Unit = { _, _ -> },
    private val onBlockedCount: (Int) -> Unit = {}
) : WebViewClient() {
    companion object {
        private val sharedClient by lazy {
            okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(false)
                .connectionSpecs(listOf(okhttp3.ConnectionSpec.MODERN_TLS))
                .build()
        }
        private val ioScope = CoroutineScope(Dispatchers.IO)
    }

    private val lock = Any()
    @Volatile private var pageHost: String? = null
    private var blockedInPage = 0
    private val injectedTabs = mutableSetOf<String>()

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url != null) {
            synchronized(lock) {
                pageHost = try { android.net.Uri.parse(url).host?.lowercase() } catch (_: Exception) { null }
                blockedInPage = 0
                injectedTabs.remove(tabId)
            }
            tabManager.onPageStarted(tabId)
            tabManager.updateTabUrl(tabId, url, view?.title?.takeIf { it.isNotBlank() } ?: url)
            onPageInfo(url, view?.title ?: url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) {
            tabManager.updateTabUrl(tabId, url, view?.title?.takeIf { it.isNotBlank() } ?: url)
            onPageInfo(url, view?.title ?: url)
        }
        val count: Int
        synchronized(lock) { count = blockedInPage }
        onBlockedCount(count)
        // Phase2: inject fingerprint & console bridge - guard duplicate per tab per page load
        synchronized(lock) {
            if (!injectedTabs.add(tabId)) return
        }
        view?.let { wv ->
            val level = tabManager.tabs.value.find { it.entity.id == tabId }?.fingerprintLevel ?: "Balanced"
            val fpScript = FingerprintInjector.script(level)
            if (fpScript.isNotBlank()) wv.evaluateJavascript(fpScript, null)
            try {
                wv.evaluateJavascript(
                    """(function(){
                        const origLog=console.log, origWarn=console.warn, origErr=console.error;
                        console.log=function(){ try{NetTraConsole.log('LOG', Array.from(arguments).join(' '))}catch(e){} origLog.apply(console, arguments);};
                        console.warn=function(){ try{NetTraConsole.log('WARN', Array.from(arguments).join(' '))}catch(e){} origWarn.apply(console, arguments);};
                        console.error=function(){ try{NetTraConsole.log('ERROR', Array.from(arguments).join(' '))}catch(e){} origErr.apply(console, arguments);};
                    })();""", null)
            } catch (_: Exception) {}
        }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        if (request == null) return null
        val url = request.url.toString()
        val lowerUrl = url.trim().lowercase()
        if (lowerUrl.startsWith("ws://") || lowerUrl.startsWith("wss://")) return null
        if (lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("data:") || lowerUrl.startsWith("file:") || lowerUrl.startsWith("content:")) {
            return WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        }
        val method = request.method ?: "GET"
        val headers = request.requestHeaders ?: emptyMap()
        val enabled = tabManager.tabs.value.find { it.entity.id == tabId }?.inspectorEnabled ?: false
        // per-tab throttling (fix #7)
        val throttlingName = tabManager.tabs.value.find { it.entity.id == tabId }?.throttling ?: "OFF"
        val profile = ThrottlingInterceptor.fromName(throttlingName)
        if (ThrottlingInterceptor.isOffline(profile)) {
            if (enabled) inspector.recordRequest(tabId, url, method, headers)
            return WebResourceResponse("text/plain", "utf-8", 503, "Offline", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        }
        ThrottlingInterceptor.maybeThrottle(profile)

        val hostSnapshot: String?
        synchronized(lock) { hostSnapshot = pageHost }
        val shouldBlock = privacyEngine.shouldBlock(url, hostSnapshot, customBlocklist)
        if (shouldBlock) {
            synchronized(lock) { blockedInPage++ }
            if (enabled) inspector.markBlocked(tabId, url, "Blocked by PrivacyEngine")
            return WebResourceResponse("text/plain", "utf-8", 204, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        }

        if (enabled) {
            val req = inspector.recordRequest(tabId, url, method, headers)
            // Phase2: re-fetch GET for body capture - only https, safe no side-effect
            val acceptVal = BodyCaptureHelper.getHeaderIgnoreCase(headers, "Accept")
            if (BodyCaptureHelper.shouldRefetch(method, acceptVal, url) && lowerUrl.startsWith("https://")) {
                ioScope.launch {
                    var resp: okhttp3.Response? = null
                    try {
                        resp = sharedClient.newCall(okhttp3.Request.Builder().url(url).build()).execute()
                        val rh = resp.headers.toMultimap().mapValues { it.value.joinToString(",") }
                        val body: String? = try {
                            resp.body?.let { rb ->
                                val source = rb.source()
                                val buffer = okio.Buffer()
                                val limit = 1024L * 1024L
                                var remaining = limit
                                while (remaining > 0) {
                                    val read = source.read(buffer, minOf(8192L, remaining))
                                    if (read == -1L) break
                                    remaining -= read
                                }
                                buffer.readString(Charsets.UTF_8)
                            }
                        } catch (_: Exception) { null }
                        inspector.updateResponse(tabId, req.id, status = resp.code, responseHeaders = rh, body = body, size = body?.toByteArray()?.size?.toLong())
                    } catch (_: Exception) {
                    } finally {
                        try { resp?.close() } catch (_: Exception) {}
                    }
                }
            }
        }
        return null
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        val trimmed = url.trim()
        val lower = trimmed.lowercase()
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("file:") || lower.startsWith("content:")) {
            return true
        }
        // HTTPS-first upgrade
        if (lower.startsWith("http://")) {
            val upgraded = privacyEngine.shouldUpgradeToHttps(trimmed)
            if (upgraded != null) {
                view?.loadUrl(upgraded)
                return true
            }
        }
        return false
    }
}
