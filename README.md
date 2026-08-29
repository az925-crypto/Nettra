# Nettra — Privacy Browser

**Package:** `com.zaaam.nettra`  
**Repo:** https://github.com/az925-crypto/Nettra

Browser Android privacy-first dari nol — default search **DuckDuckGo**, tracker blocking dari data publik `duckduckgo/tracker-blocklists`, Fire Button wipe storage, Privacy Report per situs. Bukan fork DuckDuckGo (Apache 2.0) — hanya memakai search endpoint & blocklist publik. Tidak berafiliasi resmi dengan DuckDuckGo Inc.

Dokumen konstitusi: `01_PRD.md` · `02_ARCHITECTURE.md` · `03_VERSION_MATRIX.md` · `04_DEFINITION_OF_DONE.md`

## Prinsip (notes.txt)
- Build & tes berat **hanya di GitHub Actions** — jangan di lokal.
- Selalu build release dan letakkan di **Releases**.
- Mockup HTML full app di `mockup/` — jalankan di localhost, minta approval Telegram sebelum masuk Compose.

## Arsitektur modular
```
app/
core-webview/      # WebView wrapper, NettraWebViewClient (shouldInterceptRequest)
core-privacy/      # TrackerBlocker + bundled TDS snapshot (v2026.08.21)
core-search/       # SearchRouter — URL vs query, build DuckDuckGo URL (stateless)
core-tabs/         # Room (bookmark/history) + FireWiper (CookieManager/WebStorage wipe)
feature-browser-ui/ # Compose UI (address bar, tab strip, Privacy Report, Fire)
```

## Build
```bash
./gradlew assembleDebug           # CI only (heavy)
./gradlew testDebugUnitTest       # core-search, core-privacy
./gradlew assembleRelease         # release APK/AAB → GitHub Release
```
JDK 21, Gradle 8.12, AGP 8.7.3, Kotlin 2.0.21, compileSdk 36, minSdk 26, Compose BOM 2024.12.01 — lihat `03_VERSION_MATRIX.md` + `gradle/libs.versions.toml`.

## Mockup
```bash
python3 -m http.server 8000 --directory mockup
# http://localhost:8000
```

## Legal
Tracker data © DuckDuckGo — CC BY-NC-SA 4.0 (NonCommercial). Butuh klarifikasi kalau dimonetisasi atau fallback ke EasyPrivacy. Atribusi di Tentang.
