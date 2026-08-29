package com.zaaam.nettra.webview

import android.webkit.JavascriptInterface
import com.zaaam.nettra.inspector.NetworkInspector
import com.zaaam.nettra.inspector.model.ConsoleEntry

class JsConsoleBridge(private val tabId: String, private val inspector: NetworkInspector) {
    @JavascriptInterface
    fun log(level: String, message: String) {
        inspector.addConsoleLog(tabId, ConsoleEntry(level.uppercase(), message, tabId = tabId))
    }
    @JavascriptInterface
    fun error(message: String) { log("ERROR", message) }
    @JavascriptInterface
    fun warn(message: String) { log("WARN", message) }
}
