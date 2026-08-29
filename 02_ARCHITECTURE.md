# 02_ARCHITECTURE.md

## Architecture Contract: [Nama Aplikasi — TBD]

**Versi:** 1.0
**Status:** Kontrak mengikat — agent tidak boleh mengubah struktur fundamental tanpa proses approval di Section 8.

---

## 1. Prinsip

Dokumen ini menjawab "bagaimana dibangun". Agent mengikuti arsitektur ini. Kalau ada masalah arsitektural: **STOP → jelaskan → usulkan → approval → baru implementasi.**

---

## 2. Modul

    app/
    ├── core-webview/     # Wrapper engine WebView, render halaman
    ├── core-privacy/     # Tracker blocklist matching (data DDG), HTTPS upgrade
    ├── core-search/      # Deteksi URL vs query, routing non-URL ke endpoint DuckDuckGo
    ├── core-tabs/        # Tab management, bookmark, history (Room), logic Fire Button
    └── feature-browser-ui/  # UI: address bar, tab strip, Privacy Report overlay

**Batasan antar modul:**
- `core-privacy` beroperasi di level `WebViewClient.shouldInterceptRequest`, SEBELUM request dikirim.
- `core-search` murni logic deteksi input (regex/heuristik URL) + membangun URL endpoint pencarian DuckDuckGo — tidak menyimpan state, stateless.
- `core-tabs` memegang logic Fire Button — wajib punya akses eksplisit untuk memerintahkan `core-webview` membersihkan cookie/cache/storage, bukan cuma menutup tab secara visual.

---

## 3. Data Flow

    User input (address bar)
       → feature-browser-ui
       → core-search (URL atau query? kalau query, bangun URL DuckDuckGo search)
       → core-tabs (tentukan tab aktif)
       → core-webview (load request)
            → core-privacy (cek blocklist SEBELUM request keluar)
       → feature-browser-ui (render hasil + update Privacy Report count)

    Fire Button ditekan
       → core-tabs (trigger wipe)
       → core-webview (hapus cookie/cache/storage semua instance WebView)
       → core-tabs (reset ke 1 tab kosong, history dihapus, bookmark TIDAK disentuh)
       → feature-browser-ui (render state awal)

---

## 4. State Management

- Tiap tab (`core-tabs`) menyimpan state sendiri (URL aktif, history stack, scroll position) — terisolasi, tidak ada shared mutable state antar tab.
- Counter tracker-blocked per halaman (untuk FR-7 Privacy Report) disimpan per-tab, direset setiap navigasi ke halaman baru.

---

## 5. Storage

| Data | Lokasi | Persist? |
|---|---|---|
| Bookmark, history | Room DB (`core-tabs`) | Ya — **tidak** ikut terhapus oleh Fire Button |
| Cookie, cache, local storage WebView | WebView storage bawaan | Ya, sampai Fire Button ditekan atau dihapus manual |
| Blocklist tracker DDG | Bundled asset (JSON) di `core-privacy`, versi & tanggal snapshot dicatat | Ya, di-update tiap rilis app (MVP) |
| Counter tracker-blocked per halaman | In-memory per tab | Tidak — direset tiap navigasi |

---

## 6. Networking

- Semua request halaman lewat WebView engine bawaan.
- `core-search` membangun URL ke domain `duckduckgo.com` untuk query pencarian — tidak ada endpoint API terpisah/berbayar, murni pakai halaman pencarian publik mereka.
- Tidak ada request telemetry dari app ke server developer sendiri.

---

## 7. Security

- Data blocklist DDG (`core-privacy`) di-bundle sebagai read-only asset — tidak ada mekanisme yang memungkinkan modifikasi blocklist dari luar app (mencegah tampering).
- Fire Button (FR-6) wajib menghapus data di level storage WebView asli (`WebStorage`, `CookieManager`), bukan cuma clear di level UI/state aplikasi — ini poin keamanan paling kritis di app ini dan wajib masuk security review.

---

## 8. Proses Perubahan Arsitektur

    Deteksi masalah arsitektural
            ↓
    STOP
            ↓
    Jelaskan masalah + dampak ke modul lain
            ↓
    Usulkan perubahan
            ↓
    Review & approval
            ↓
    02_ARCHITECTURE.md diupdate dulu, baru implementasi

---

## 9. Testing Strategy

| Layer | Jenis Test | Wajib untuk |
|---|---|---|
| `core-privacy` | Unit test — blocklist matching logic terhadap data DDG asli | P0 |
| `core-search` | Unit test — deteksi URL vs query, benar membangun URL DuckDuckGo | P0 |
| `core-tabs` (Fire Button) | Integration test — cookie/cache benar-benar terhapus dari storage nyata setelah trigger, bukan cuma state di-reset | P0 |
| Alur end-to-end (FR-2, FR-4, FR-6) | Manual/instrumented test di emulator/device fisik | P0, wajib sebelum DONE |
