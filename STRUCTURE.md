# STRUCTURE — NetTra

## Modul
- **app** (`com.zaaam.nettra`): `NetTraApp`, `MainActivity`, `ui/BrowserScreen.kt` (TabStrip, AddressBar, WebView, InspectorSheet, Detail Tabs), `ui/theme/Theme.kt`, `AndroidManifest.xml`, `network_security_config.xml`
- **core-tabs** (`com.zaaam.nettra.tabs`): `model/TabEntity.kt`, `db/TabDao.kt`, `db/NetTraDatabase.kt` (version 1, exportSchema true), `TabManager.kt` (StateFlow, 5 tab cap, delegasi ke NetworkInspector)
- **core-privacy** (`com.zaaam.nettra.privacy`): `PrivacyEngine.kt` (blocklist, shouldBlock lowercase, shouldUpgradeToHttps), `CookiePolicy.kt`, `HttpsUpgrader.kt`
- **core-network-inspector** (`com.zaaam.nettra.inspector`): `model/CapturedRequest.kt`, `model/ResourceType.kt`, `HeaderMasking.kt` (mask + scrubUrl), `NetworkInspector.kt` (300/tab LRU, 100KB preview, 1MB hard limit, StateFlow, synchronized)
- **core-webview** (`com.zaaam.nettra.webview`): `NetTraWebViewClient.kt` (shouldInterceptRequest, block 204, ws/wss skip, pageHost synchronized, HTTPS upgrade)

## Data Flow
`TabManager` (selectedId) → `BrowserScreen` (WebView per tabId, remember+DisposableEffect) → `NetTraWebViewClient.shouldInterceptRequest` → `PrivacyEngine.shouldBlock` → if block return 204 else `NetworkInspector.recordRequest` (masked+scrubbed) → `NetworkInspector.getLogFlow` → `InspectorSheet` (filter/search/waterfall/summary)

## Build
- `settings.gradle.kts` include 5 modul, `gradle/libs.versions.toml` BOM 2024.12.01, AGP 9.3.0, Kotlin 2.4.10
- `gradle.properties` caching, `gradlew` wrapper 9.5.0
- CI tidak di lokal, heavy tasks di GitHub Actions
