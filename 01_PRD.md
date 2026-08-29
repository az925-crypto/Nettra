# 01_PRD.md

## PRD: [Nama Aplikasi — TBD]
### Privacy Browser — DuckDuckGo Search + Tracker Data, Dibangun dari Nol

**Versi:** 1.0 — Contract-Grade
**Tanggal:** 27 Agustus 2026
**Status:** Draft — bagian dari 4 dokumen konstitusi proyek (lihat 02_ARCHITECTURE.md, 03_VERSION_MATRIX.md, 04_DEFINITION_OF_DONE.md)

> **Klarifikasi penting:** app ini **bukan** fork dari source code DuckDuckGo Android (meski itu open source, Apache 2.0). App dibangun dari nol dengan codebase sendiri, tapi memakai dua aset publik DuckDuckGo secara konkret: (1) DuckDuckGo sebagai default search engine, (2) data tracker blocklist publik mereka (`duckduckgo/tracker-blocklists`) sebagai sumber blocking. App ini **tidak berafiliasi resmi** dengan DuckDuckGo Inc.

---

## 1. Ringkasan Eksekutif

Browser Android privacy-first yang dibangun dari nol, memakai DuckDuckGo sebagai mesin pencari default dan data tracker publik mereka sebagai sumber blocking, dilengkapi fitur signature ala DuckDuckGo: Fire Button (hapus semua data browsing sekali tap) dan Privacy Report per situs.

---

## 2. Masalah & User Story

User yang mau browsing privat tapi tidak mau/tidak bisa pakai app DuckDuckGo resmi (misal karena mau kontrol penuh atas kode, atau mau kombinasi fitur custom) butuh browser independen yang tetap mengadopsi pendekatan privacy DuckDuckGo — search tanpa profiling, tracker blocking otomatis, dan cara mudah membersihkan jejak browsing.

**User story:** User yang mau browsing sehari-hari tanpa merasa "diikutin" — search-nya default ke DuckDuckGo bukan Google, tracker keblokir otomatis, dan kalau mau "bersih-bersih total" cukup satu tap.

---

## 3. Target User

- User privacy-conscious yang mau default aman tanpa konfigurasi manual
- User yang familiar dengan konsep DuckDuckGo (search tanpa tracking, Fire Button) dan mau pengalaman serupa di browser independen

**Bukan target (v1):** user yang butuh Email Protection (Duck Address) atau App Tracking Protection device-wide — dua fitur ini butuh backend/akun resmi DuckDuckGo dan tidak bisa direplikasi independen (lihat Section 8).

---

## 4. Prinsip Prioritas (WAJIB DIPATUHI AGENT)

- **P0 = Core/Critical.** Tanpa ini produk tidak berfungsi. P0 harus lolos acceptance criteria + verifikasi sebelum P1 dikerjakan.
- **P1 = Important.** Tidak boleh dikerjakan sebelum semua P0 lolos.
- **P2 = Nice to have.** Tidak boleh mendahului P0/P1.
- Status task hanya **DONE** atau **NOT DONE** — tidak ada persentase.

---

## 5. Functional Requirements — Browser Dasar

### FR-1: Multi-tab Browsing — **P0**
- **Apa:** User dapat membuka, berpindah, dan menutup tab browsing.
- **Perilaku yang diharapkan:** New tab = instance WebView baru independen; switching tab mempertahankan state masing-masing; closing tab benar-benar membebaskan resource; minimal satu tab selalu tersedia.
- **Mock diperbolehkan:** **Tidak.**
- **Acceptance Criteria:**
  - [ ] Tab independen satu sama lain (load URL beda tanpa saling pengaruh)
  - [ ] Switching tab mempertahankan scroll position & history masing-masing
  - [ ] Closing tab membebaskan resource WebView-nya
  - [ ] Tidak bisa berada di kondisi 0 tab
- **Verifikasi:** Buka 3 tab beda URL, scroll masing-masing berbeda, pindah-pindah, pastikan posisi scroll tiap tab tetap konsisten; tutup satu tab, pastikan yang lain tidak terpengaruh.

### FR-2: Address Bar & Default Search = DuckDuckGo — **P0**
- **Apa:** User memasukkan URL atau search query; kalau bukan URL valid, otomatis dikirim sebagai pencarian ke DuckDuckGo (bukan Google/Bing).
- **Kenapa:** Ini titik paling konkret dari "pakai DuckDuckGo" di app ini — search default yang tidak melakukan profiling user.
- **Perilaku yang diharapkan:** Input dideteksi apakah URL valid atau bukan; kalau bukan, diarahkan ke endpoint pencarian DuckDuckGo; hasil pencarian tampil di WebView seperti biasa; loading & error state jelas.
- **Mock diperbolehkan:** **Tidak.**
- **Acceptance Criteria:**
  - [ ] Input non-URL (misal "resep nasi goreng") menghasilkan halaman hasil pencarian dari domain `duckduckgo.com`, bukan mesin pencari lain
  - [ ] Input URL valid langsung dianggap navigasi, tidak dikirim sebagai query pencarian
  - [ ] Tidak ada default lain (Google/Bing) tanpa perubahan setting eksplisit dari user
  - [ ] Back/forward navigation bekerja normal
- **Verifikasi:** Ketik keyword non-URL di address bar, submit, pastikan halaman yang termuat benar-benar berasal dari `duckduckgo.com` — cek lewat address bar hasil akhir setelah redirect.

### FR-3: Bookmark & History Lokal — **P1**
- **Acceptance Criteria:** Bookmark & history persist antar sesi app (Room DB), tidak hilang setelah restart, tidak ikut terhapus oleh Fire Button (FR-6).
- **Verifikasi:** Simpan bookmark → force-close app → buka lagi → bookmark masih ada.

---

## 6. Functional Requirements — Privacy Core (Berbasis Data DuckDuckGo)

### FR-4: Tracker Blocking pakai Data DuckDuckGo — **P0**
- **Apa:** Request ke domain pihak ketiga yang teridentifikasi sebagai tracker diblokir otomatis, berdasarkan data dari `duckduckgo/tracker-blocklists` (blocklist `web`).
- **Kenapa:** Ini alasan teknis utama app ini "berbasis DuckDuckGo" — memakai data tracker yang mereka maintain terus-menerus lewat Tracker Radar, bukan bikin blocklist sendiri dari nol.
- **Perilaku yang diharapkan:** Blocklist JSON di-bundle sebagai snapshot bertanggal di dalam app; dicocokkan di level `shouldInterceptRequest` sebelum request keluar; domain yang match diblokir.
- **Mock diperbolehkan:** **Tidak untuk P0.**
- **Acceptance Criteria:**
  - [ ] Domain yang ada di blocklist DDG benar-benar diblokir requestnya, bukan cuma UI toggle kosong
  - [ ] Domain first-party / bukan tracker tidak ikut ke-block (tidak ada false positive yang merusak fungsi situs)
  - [ ] Versi & tanggal snapshot blocklist tercatat jelas di dalam app untuk keperluan audit
- **Verifikasi:** Kunjungi halaman yang diketahui memuat tracker umum yang ada di blocklist DDG, pastikan request ke domain tracker tersebut tidak pernah terkirim (dicek lewat logging debug internal).

> **Catatan lisensi (WAJIB dibaca sebelum lock-in):** data Tracker Radar/blocklist DuckDuckGo berlisensi **CC BY-NC-SA 4.0 (NonCommercial)**. Kalau app ini nantinya dimonetisasi (iklan, berbayar, in-app purchase), pemakaian data ini untuk tujuan komersial **butuh klarifikasi lisensi terpisah dari DuckDuckGo**, atau ganti ke sumber blocklist lain yang lebih permisif (misal EasyPrivacy) sebagai fallback. Lihat Section 11 Keputusan Terbuka.

### FR-5: HTTPS-First (Smarter Encryption) — **P0**
- **Acceptance Criteria:** Situs yang support HTTPS otomatis di-upgrade dari HTTP; situs HTTP polos dapat warning jelas sebelum lanjut.
- **Verifikasi:** Akses versi HTTP dari situs yang support HTTPS → pastikan otomatis redirect ke HTTPS.

### FR-6: Fire Button — **P0**
- **Apa:** Satu tombol yang langsung menghapus semua data sesi browsing (semua tab, cookie, cache) secara instan.
- **Kenapa:** Fitur signature paling dikenal dari DuckDuckGo — sinyal privasi yang kuat dan mudah dipahami user awam sekalipun.
- **Perilaku yang diharapkan:** Tap Fire Button → semua tab tertutup, app kembali ke satu tab kosong; cookie, cache, dan local storage WebView benar-benar dibersihkan; bookmark **tidak** ikut terhapus (itu data tersimpan permanen milik user, bukan data sesi).
- **Mock diperbolehkan:** **Tidak.** Ini requirement paling rawan "kelihatan jalan tapi sebenarnya tidak" — tab keliatan tertutup tapi cookie/cache masih nyangkut adalah kegagalan tersembunyi.
- **Acceptance Criteria:**
  - [ ] Semua tab tertutup dan direset ke 1 tab kosong
  - [ ] Cookie & cache WebView benar-benar terhapus dari storage, bukan cuma UI yang di-reset
  - [ ] History browsing ikut terhapus (konsisten dengan perilaku Fire Button DuckDuckGo asli)
  - [ ] Bookmark TIDAK ikut terhapus
- **Verifikasi:** Browsing di beberapa tab, buka situs yang diketahui set cookie, tekan Fire Button, lalu inspeksi storage WebView (lewat adb/debug tool) — pastikan cookie & cache benar-benar kosong, bukan cuma tab yang tertutup secara visual.

### FR-7: Privacy Report per Situs — **P1**
- **Apa:** Indikator di address bar menunjukkan jumlah tracker yang diblokir dan status enkripsi untuk halaman aktif.
- **Mock diperbolehkan:** Tidak — angka harus berasal dari data nyata FR-4, bukan statis/acak.
- **Acceptance Criteria:**
  - [ ] Angka tracker yang ditampilkan sama persis dengan jumlah request yang benar-benar diblokir FR-4 pada halaman itu
  - [ ] Status enkripsi (HTTPS/HTTP) sesuai kondisi koneksi nyata
- **Verifikasi:** Bandingkan angka di Privacy Report dengan log internal jumlah blocking dari FR-4 di halaman yang sama.

### FR-8: Private Tab — **P2**
- **Acceptance Criteria:** Data private tab terhapus otomatis begitu tab ditutup, terlepas dari kapan Fire Button ditekan.

---

## 7. Phase 2 — Nice to Have

| Fitur | Catatan |
|---|---|
| Update blocklist otomatis berkala dari repo publik DDG | MVP pakai snapshot statis dulu (lihat Keputusan Terbuka #2) |
| Cookie pop-up auto-dismiss | Fitur yang juga ada di DuckDuckGo asli, independen dari data tracker |
| Duck Player-lite (privacy-respecting video embed) | Kompleksitas tinggi, proxy/embed handling terpisah |
| Custom user-added block rules | Di atas fondasi FR-4 yang sudah stabil |

**Eksplisit di luar scope (tidak bisa direplikasi independen):**
- Email Protection (Duck Address) — butuh backend & akun resmi DuckDuckGo
- App Tracking Protection (VPN-based, device-wide) — di luar scope browser, butuh implementasi VPN service Android terpisah yang jauh lebih kompleks

---

## 8. Edge Cases & Error Handling

| Skenario | Perilaku yang diharapkan |
|---|---|
| Endpoint pencarian DuckDuckGo tidak bisa diakses (down/no internet) | Pesan error jelas, bukan blank/crash |
| Blocklist gagal dimuat saat startup | Fallback: browsing tetap jalan tanpa blocking, beri notifikasi jelas bahwa proteksi tidak aktif — jangan diam-diam browsing tanpa proteksi tanpa pemberitahuan |
| Fire Button ditekan saat ada download/proses aktif | Konfirmasi dulu sebelum eksekusi, jangan langsung potong proses tanpa peringatan |

---

## 9. Diferensiasi

| Aspek | App ini | DuckDuckGo asli | Brave |
|---|---|---|---|
| Search default | DuckDuckGo | DuckDuckGo | Brave Search (bisa diganti) |
| Sumber data tracker blocking | Data publik DDG (`tracker-blocklists`) | Native, terintegrasi penuh | Sumber sendiri (Brave Shields) |
| Fire Button | Ya | Ya | Konsep beda ("Clear browsing data") |
| Afiliasi resmi dengan DuckDuckGo Inc. | **Tidak** | Ya (produk resmi) | Tidak |

---

## 10. Legal & Attribution

- App ini memakai **data publik** DuckDuckGo (search engine, tracker blocklist), bukan source code atau brand mereka.
- App **wajib** memakai nama dan branding sendiri — **tidak boleh** memakai nama "DuckDuckGo", logo bebek, atau klaim afiliasi resmi apapun dengan DuckDuckGo Inc.
- Atribusi sumber data (Tracker Radar/blocklist) sebaiknya dicantumkan di halaman "Tentang" app, sesuai etika penggunaan data terbuka.

---

## 11. Keputusan Terbuka

1. **Lisensi data Tracker Radar/blocklist (CC BY-NC-SA 4.0, NonCommercial)** — wajib diklarifikasi ke DuckDuckGo kalau app akan dimonetisasi; siapkan fallback ke blocklist lain (mis. EasyPrivacy) kalau tidak dapat izin komersial
2. Mekanisme update blocklist — snapshot statis di-bundle per rilis (MVP) vs fetch berkala dari repo publik (Phase 2)
3. Nama & branding app (wajib independen, lihat Section 10)
4. Engine WebView — system WebView vs custom Chromium build

---

*Dokumen ini bagian dari 4 kontrak proyek. Agent WAJIB membaca 02_ARCHITECTURE.md, 03_VERSION_MATRIX.md, dan 04_DEFINITION_OF_DONE.md sebelum mulai implementasi apapun.*
