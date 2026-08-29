# PRD: [Nama Aplikasi — TBD]
### Privacy-First Browser dengan Network Inspector

**Versi:** 1.0 (Draft)
**Tanggal:** 27 Agustus 2026
**Status:** Draft — siap direview / di-feed ke planner agent

---

## 1. Ringkasan Eksekutif

Browser Android yang privacy-first secara default (block tracker, block third-party cookie, tanpa telemetry sendiri), dilengkapi Network Inspector built-in ala DevTools — developer bisa lihat request/response dari halaman yang lagi dibuka, langsung dari HP, tanpa perlu tethering ke desktop Chrome (`chrome://inspect`).

---

## 2. Masalah

Developer yang mau debug halaman web/API dari HP saat ini kesulitan: Chrome mobile tidak punya panel inspeksi network yang bisa diakses langsung di HP — satu-satunya cara resmi adalah remote debugging via USB ke desktop Chrome. Di sisi lain, browser privacy-first yang ada (Brave, Firefox Focus) fokus ke blocking tracker tapi tidak punya kemampuan inspeksi network sama sekali.

**User story:**
Developer yang lagi testing web app atau API dari HP — mau ngecek response header, status code, atau body dari sebuah request tanpa harus nyambungin HP ke laptop dulu. Di saat yang sama, tetap mau browsing hariannya privat, tanpa tracker ngikutin.

---

## 3. Target User

- Web/mobile developer yang sering testing/debug halaman atau API langsung dari HP
- User privacy-conscious yang mau browser default aman tanpa perlu konfigurasi manual

**Bukan target (v1):**
- User yang cuma butuh browser ringan-cepat tanpa fitur developer (positioning app ini memang developer-first)
- Kebutuhan capture traffic *device-wide* dari app lain (VPN-based packet capture) — app ini cuma inspect traffic dari WebView-nya sendiri, bukan system-wide network monitor

---

## 4. Tujuan & Metrik Sukses

| Tujuan | Metrik | Target |
|---|---|---|
| Debug cepat tanpa tethering | Tap dari buka Network Inspector ke lihat detail request | < 2 tap |
| Privacy kuat secara default | Tracker diblokir otomatis tanpa setup manual | 100% default-on |
| Browser sendiri auditable | Request telemetry yang dikirim browser ke server sendiri | 0 |
| Ringan di device low-end | Overhead RAM saat Network Inspector aktif | Diukur & divalidasi di Fase 0 |

---

## 5. Scope & Prioritas

### MVP (v1)

| Fitur | Prioritas |
|---|---|
| Browser dasar: multi-tab, address bar, bookmark, history lokal | P0 |
| Block tracker & third-party cookie by default | P0 |
| HTTPS-first / upgrade otomatis | P0 |
| Network Inspector: list request gaya DevTools (name, status, type, size, time + waterfall bar) | P0 |
| Detail request pakai tab: Headers / Preview / Response / Timing (gaya DevTools) | P0 |
| Filter by resource type (chip: All/Fetch-XHR/JS/CSS/Img/Media/Font/Doc) + search | P0 |
| Masking otomatis untuk header sensitif (Authorization, Cookie, dll) | P0 |
| Private tab (data wipe otomatis setelah sesi) | P1 |

### Phase 2 — nice-to-have

| Fitur | Prioritas |
|---|---|
| Response body inspection penuh (termasuk JSON pretty-print) | P2 (tergantung hasil Fase 0) |
| Request replay/modify (kirim ulang request dgn header/body diubah) | P2 |
| Export capture sebagai file HAR | P2 |
| Fingerprint protection lanjutan (canvas/audio fingerprint blocking) | P2 |
| Custom ad-block filter list | P2 |
| Console tab — lihat `console.log`/error dari halaman, execute JS snippet | P2 |
| Network throttling simulation (Slow 3G/Fast 3G) buat testing performa | P2 |

### Eksplisit di luar scope
- Full desktop-parity DevTools (CPU profiler, memory heap snapshot)
- Ekstensi/plugin pihak ketiga
- Sync akun/data lintas device (privacy-first — hindari akun cloud di MVP)
- Packet capture device-wide (di luar traffic browser ini sendiri)

---

## 6. Functional Requirements

**Browser Dasar**
- **FR-1:** Multi-tab browsing
- **FR-2:** Address bar dengan search/URL, indikator koneksi HTTPS
- **FR-3:** Bookmark & history tersimpan lokal (tidak ada sync cloud)

**Network Inspector (gaya Chrome DevTools)**
- **FR-4:** Capture semua network request yang di-trigger halaman aktif (XHR/Fetch, JS, CSS, Img, Media, Font, Doc)
- **FR-5:** List request dengan kolom: name/URL, status, type, size, time, plus indikator waterfall/timing bar per baris
- **FR-6:** Filter by resource type (chip: All/Fetch-XHR/JS/CSS/Img/Media/Font/Doc/Other) + search by URL
- **FR-7:** Detail request pakai tab terpisah — **Headers** (general/request/response headers), **Preview** (response di-render: JSON tree/gambar), **Response** (raw body — lihat Section 10 soal keterbatasan), **Timing** (breakdown queueing/DNS/connecting/TTFB/download)
- **FR-8:** Summary bar: total request, total size transferred, waktu load halaman
- **FR-9:** "Preserve log" toggle — log tidak hilang saat navigasi ke halaman baru dalam tab yang sama
- **FR-10:** Toggle Network Inspector on/off per tab, biar hemat resource kalau tidak dipakai
- **FR-11:** Header sensitif (`Authorization`, `Cookie`, `Set-Cookie`, pola `api-key`) di-mask default, ada tombol "show" per header

**Privacy**
- **FR-12:** Block third-party tracker by default (pakai blocklist yang di-maintain, mis. gaya EasyPrivacy)
- **FR-13:** Block third-party cookie by default
- **FR-14:** Force upgrade ke HTTPS bila tersedia, warning jelas untuk koneksi HTTP polos
- **FR-15:** Browser sendiri tidak mengirim telemetry/analytics apapun — bisa diverifikasi user lewat Network Inspector-nya sendiri
- **FR-16:** Private tab menghapus semua data (cookie, cache, history) otomatis setelah sesi ditutup

---

## 7. Alur Utama (User Flows)

**Alur browsing biasa**
1. Buka app → tab kosong/homepage
2. Ketik URL/search → halaman load, tracker otomatis ke-block di background
3. Icon shield di address bar nunjukin jumlah tracker yang diblokir di halaman itu

**Alur debug (Network Inspector)**
1. Buka halaman yang mau di-debug
2. Tap tombol Network Inspector → panel muncul, list request yang sudah/lagi ke-capture (bisa difilter by type)
3. Tap salah satu request → lihat detail lewat tab Headers / Preview / Response / Timing (gaya DevTools)
4. Header sensitif otomatis ke-mask, tap "show" kalau memang perlu dilihat

---

## 8. Non-Functional Requirements

- **Performa:** buffer capture per tab dibatasi (auto-clear tab lama/tidak aktif), biar tidak membengkak di RAM 8GB/Snapdragon 680
- **Memory:** response body besar (gambar/video/streaming) tidak disimpan penuh di memory — cukup metadata + preview terpotong
- **Security:** data request/response yang di-capture cuma in-memory per sesi, tidak persist ke disk secara default — mengurangi risiko token/credential ke-log tanpa sadar
- **Compatibility:** target Android 8 (API 26) ke atas

---

## 9. Arsitektur Teknis

**Stack:** Kotlin + Jetpack Compose, Room (bookmark & history lokal)

| Modul | Tanggung Jawab |
|---|---|
| `core-webview` | Wrapper engine WebView, render halaman |
| `core-network-inspector` | Intercept & log request/response per tab |
| `core-privacy` | Tracker blocklist matching, cookie policy, HTTPS upgrade |
| `core-tabs` | Tab management, bookmark, history (Room) |

---

## 10. Keputusan Teknis Kritis: Pendekatan Network Inspector

Ini keputusan arsitektur paling menentukan di project ini, karena Android WebView standar **tidak secara native mengekspos response body** dengan mudah.

**Opsi A — `WebViewClient.shouldInterceptRequest`**
- Bisa lihat: URL, method, request headers
- Keterbatasan: response headers & body tidak otomatis tersedia — perlu effort ekstra untuk capture sisi response
- Kompleksitas: rendah, cocok untuk MVP cepat

**Opsi B — Local proxy (MITM lokal) dengan custom CA**
- Bisa capture request DAN response secara penuh (setara HAR)
- Keterbatasan: butuh install root CA certificate lokal di device — perlu disclosure jelas ke user, dan gagal untuk situs yang pakai certificate pinning
- Kompleksitas: tinggi, tapi hasil paling lengkap

**Catatan soal tab Timing (FR-7):** breakdown granular (DNS/connecting/TTFB/download) jauh lebih mudah didapat dari Opsi B, karena Opsi A umumnya cuma kasih durasi total start-to-finish tanpa breakdown per fase. Kalau "pengalaman kek DevTools" ini prioritas utama, ini nambah bobot ke arah Opsi B — meski kompleksitasnya lebih tinggi.

**Rekomendasi:** validasi kedua opsi di Fase 0 sebelum commit ke salah satu untuk MVP. Kalau Opsi A dipilih untuk v1, response body inspection penuh & Timing breakdown granular masuk Phase 2 dengan Opsi B.

---

## 11. Keamanan & Privasi

- **Kalau pakai local proxy (Opsi B):** CA certificate wajib di-scope hanya untuk traffic dari WebView app ini sendiri — tidak pernah dipakai untuk intercept traffic app lain. User harus lihat disclosure jelas sebelum CA diinstal, dan bisa cabut kapan saja
- **Data capture:** tidak persist ke disk default, header sensitif ter-mask default (lihat FR-9)
- **Privacy browser:** tidak ada akun wajib, tidak ada sync cloud di MVP — semua data lokal di device

---

## 12. Edge Cases & Error Handling

| Skenario | Perilaku yang diharapkan |
|---|---|
| Request berukuran sangat besar (streaming video, dll) | Tidak capture full body — cukup metadata + ukuran, tandai "terlalu besar untuk ditampilkan" |
| Situs pakai WebSocket / request non-HTTP | Tampilkan di list terpisah kalau bisa di-capture, atau exclude dengan catatan jelas di MVP |
| Banyak tab aktif dengan Inspector menyala bersamaan | Batasi jumlah tab yang capture bersamaan, biar tidak OOM di device RAM kecil |
| Situs pakai certificate pinning (kalau pakai Opsi B) | Gagal intercept HTTPS-nya — tampilkan pesan jelas, bukan crash diam-diam |
| Response bukan text/JSON (binary/gambar) | Tampilkan preview gambar kalau image, atau placeholder "binary content" |

---

## 13. Risiko & Mitigasi

| Risiko | Mitigasi |
|---|---|
| WebView standar tidak expose response body secara native | Validasi Opsi A vs B di Fase 0 sebelum commit scope MVP |
| Local proxy + custom CA menimbulkan concern trust dari user | Scope CA ketat, disclosure transparan, opsional bukan wajib aktif |
| Data capture menyimpan token/credential tanpa sadar | Masking default untuk header sensitif (FR-9) |
| Overhead performa dari intercept semua request di device low-end | Buffer terbatas per tab, auto-clear, Inspector bisa dimatikan total |

---

## 14. Diferensiasi

| Aspek | App ini | Chrome Mobile | Kiwi Browser |
|---|---|---|---|
| Network Inspector tanpa tethering desktop | Ya | Tidak (perlu USB + `chrome://inspect`) | Tidak native |
| Privacy-first by default | Ya | Tidak | Tergantung extension yang diinstal manual |
| Setup awal | Langsung pakai | - | Perlu cari & install extension |

---

## 15. Keputusan Terbuka & Fase Pengembangan

**Keputusan terbuka:**
1. Opsi A vs Opsi B untuk Network Inspector (lihat Section 10)
2. Engine WebView — system WebView (ringan, kontrol terbatas) vs custom Chromium build (kontrol penuh, ukuran app jauh lebih besar)
3. Network Inspector aktif default vs opt-in manual per tab
4. Nama & positioning app

**Fase pengembangan:**
- **Fase 0 — Validasi teknis:** spike Opsi A vs Opsi B, ukur overhead performa & kelengkapan data yang bisa ditangkap, putuskan pendekatan final
- **Fase 1 — MVP:** browser dasar + fitur privasi (FR-10 s/d FR-14) + Network Inspector sesuai hasil Fase 0
- **Fase 2 — Enhancement:** response body penuh (bila belum ada di v1), request replay/modify, export HAR, fingerprint protection lanjutan

