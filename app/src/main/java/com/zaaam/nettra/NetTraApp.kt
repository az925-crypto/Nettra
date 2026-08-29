package com.zaaam.nettra

import android.app.Application
import android.os.Build
import android.webkit.WebView
import com.zaaam.nettra.tabs.TabManager
import com.zaaam.nettra.inspector.NetworkInspector
import com.zaaam.nettra.privacy.PrivacyEngine

class NetTraApp : Application() {
    val inspector by lazy { NetworkInspector() }
    val tabManager by lazy { TabManager(inspector) }
    val privacyEngine by lazy { PrivacyEngine() }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { WebView.setDataDirectorySuffix("nettra") } catch (_: Exception) {}
        }
    }
}
