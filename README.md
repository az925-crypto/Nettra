# NetTra — Privacy-First Browser + Network Inspector

Privacy-first browser Android (block tracker, block third-party cookie, HTTPS-first, zero telemetry) dengan Network Inspector built-in ala DevTools — debug request/response langsung dari HP tanpa `chrome://inspect`.

**Package:** `com.zaaam.nettra` · **MinSdk:** 26 · **Stack:** Kotlin + Jetpack Compose + Room · **Repo:** https://github.com/az925-crypto/Editors.git

## MVP (P0) — Selesai
- Browser dasar: multi-tab, address bar, bookmark/history lokal (Room), private tab (wipe cookie/cache/storage/inspector on close)
- Privacy: block tracker (blocklist `core-privacy`), block third-party cookie via `CookieManager.setAcceptThirdPartyCookies`, HTTPS-first upgrade (case-insensitive), shield badge blocked count, `networkSecurityConfig` cleartext false, `allowBackup=false`
- Network Inspector Opsi A (`WebViewClient.shouldInterceptRequest`): list request (name/status/type/size/time + waterfall), filter chips All/Fetch-XHR/JS/CSS/Img/Media/Font/Doc + search, detail tabs Headers/Preview/Response/Timing, masking header sensitif (`Authorization/Cookie/Set-Cookie/api-key` + query param scrub), preserve log toggle, per-tab enable, summary bar, 300 req/tab LRU + 100KB preview + 1MB hard limit (in-memory only, tidak persist)
- Edge: file:// / javascript: / data: di-block (`allowFileAccess=false`), WebSocket di-skip, large body `too large`, 5 tab concurrent capture limit

Phase 2 (P2) — Selesai
- Response body full: GET re-fetch via OkHttp (1MB limit, streaming) + JsonPretty, Preview/Response tab full pretty
- Replay: ReplayEngine (OkHttp) + ReplayDialog (method/url/headers/body override) + badge replay
- HAR: HarExporter (spec 1.2) + FileProvider share `cache/har/nettra-*.har`
- Fingerprint: FingerprintInjector (Balanced/Strict) via evaluateJavascript, per-tab level
- Custom blocklist: CustomBlocklistManager (DataStore) + Room custom_filters v2 (MIGRATION_1_2) + PrivacyEngine merge
- Console: JsConsoleBridge addJavascriptInterface + NetworkInspector consoleLogs 200 cap + ConsoleTab dengan execute JS
- Throttling: ThrottlingInterceptor per-tab (OFF/SLOW_3G/FAST_3G/OFFLINE) + UI selector, waterfall striped, offline 503 tetap record

## Struktur Modul
```
nettra/
├── app/                     # :app Compose, MainActivity, BrowserScreen, InspectorSheet
├── core-tabs/               # Room TabEntity/Bookmark/History, TabManager (single source inspector)
├── core-privacy/            # PrivacyEngine, CookiePolicy, HttpsUpgrader
├── core-network-inspector/  # CapturedRequest, ResourceType, HeaderMasking, NetworkInspector (StateFlow)
├── core-webview/            # NetTraWebViewClient (shouldInterceptRequest, block, HTTPS upgrade)
├── mockup/                  # mockup/index.html — HTML full app, serve di localhost:8000
├── .github/workflows/       # ci.yml (assembleDebug + test + lint) + release.yml (assembleRelease)
└── release/                 # APK release hasil CI (jangan build lokal, lihat notes.txt)
```

## Mockup
Sebelum Compose, mockup HTML di `mockup/index.html` — jalankan:
```bash
python3 -m http.server 8000 --directory mockup
# buka http://localhost:8000
```
Desain: Void Ink `#0B0F14` + Ledger `#F2F4F7` + Slate `#6B7A90` + Pulse Amber `#FFC145` + Shield Teal `#00C2A8`, Signal Thread spine, JetBrains Mono untuk inspector.

## Build & CI (notes.txt)
- **Jangan jalankan gradlew berat di lokal** — semua `assembleDebug/assembleRelease/test/lint` di GitHub Actions.
- CI: `ci.yml` on push/PR → `./gradlew assembleDebug testDebugUnitTest lintDebug`
- Release: `release.yml` on tag `v*` → `./gradlew assembleRelease` + upload ke GitHub Release sebagai `nettra-<version>.apk` + copy ke `release/`

## Keamanan
- In-memory inspector only, header masked default + eye toggle per header, query param scrub.
- WebView hardening: `allowFileAccess=false`, `safeBrowsingEnabled=true`, `MIXED_CONTENT_NEVER_ALLOW`, `isUrlAllowed` whitelist http/https/about:blank saja.
- Private tab: `inspector.clear()`, `CookieManager.removeAllCookies()`, `WebStorage.deleteAllData()`, `WebView.clearCache(true)` di `onDestroy`.

## Pengujian
- Unit: `PrivacyEngineTest`, `HeaderMaskingTest`, `NetworkInspectorTest` (core-privacy/inspector) — dijalankan di CI `testDebugUnitTest`.
- Manual: buka `https://example.com`, aktifkan Inspektor, cek filter, detail, masking, private tab wipe.

## Roadmap
- Fase 0: validasi Opsi A vs B (selesai — Opsi A enhanced untuk P2, Opsi B CA proxy stub disclosure)
- Fase 1: MVP P0 (selesai)
- Fase 2: P2 7 fitur selesai (di atas)
- Fase 3: Opsi B CA full proxy (jika dibutuhkan) + sync + extension

## Lisensi
Private — zaaam/nettra.
