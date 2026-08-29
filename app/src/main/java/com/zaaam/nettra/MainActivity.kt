package com.zaaam.nettra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.zaaam.nettra.ui.BrowserScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as NetTraApp
        setContent {
            MaterialTheme {
                BrowserScreen(
                    tabManager = app.tabManager,
                    privacyEngine = app.privacyEngine,
                    inspector = app.inspector
                )
            }
        }
    }

    override fun onDestroy() {
        val app = application as? NetTraApp
        app?.let {
            it.tabManager.closePrivateTabs()
            it.inspector.clearAll()
            com.zaaam.nettra.privacy.CookiePolicy.clearAllCookies()
            try { android.webkit.WebStorage.getInstance().deleteAllData() } catch (_: Exception) {}
            try { android.webkit.WebView(it).clearCache(true) } catch (_: Exception) {}
            try { android.webkit.WebView.clearClientCertPreferences(null) } catch (_: Exception) {}
        }
        super.onDestroy()
    }
}
