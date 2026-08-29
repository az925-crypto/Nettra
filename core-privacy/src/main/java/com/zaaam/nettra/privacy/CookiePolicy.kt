package com.zaaam.nettra.privacy

import android.webkit.CookieManager
import android.webkit.WebView

object CookiePolicy {
    fun applyToWebView(webView: WebView) {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        try { cm.setAcceptThirdPartyCookies(webView, false) } catch (_: Exception) {}
    }

    fun clearAllCookies(onDone: (() -> Unit)? = null) {
        try {
            CookieManager.getInstance().removeAllCookies { onDone?.invoke() }
            CookieManager.getInstance().flush()
        } catch (_: Exception) { onDone?.invoke() }
    }
}
