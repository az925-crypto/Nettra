# 04_DEFINITION_OF_DONE.md

## Definition of Done: [Nama Aplikasi — TBD]

**Prinsip:** Status task hanya **DONE** atau **NOT DONE**. Tidak ada "90% selesai".

---

## 1. Global Definition of Done (semua task P0)

- [ ] Project berhasil di-build tanpa error
- [ ] App berjalan di emulator/device fisik
- [ ] Fungsi utama benar-benar bekerja, bukan cuma UI-nya ada
- [ ] Tidak pakai mock/dummy untuk fungsi utama (kecuali berlabel MOCK eksplisit)
- [ ] Loading & error state bekerja, bukan cuma happy path
- [ ] Tidak ada TODO berlabel P0 tersisa
- [ ] Fitur sebelumnya yang sudah DONE tidak jadi rusak (regression)
- [ ] Acceptance criteria di 01_PRD.md lolos semua
- [ ] Sudah diverifikasi manual sesuai langkah di 01_PRD.md

---

## 2. Task-Level DoD — Requirement Paling Kritis

### TASK: Implement FR-6 — Fire Button

Task hanya DONE apabila:
- [ ] Semua tab tertutup, direset ke 1 tab kosong
- [ ] Cookie & cache WebView **benar-benar terhapus dari storage nyata** — dibuktikan lewat inspeksi storage (adb/debug tool), bukan cuma tab tertutup secara visual
- [ ] History browsing ikut terhapus
- [ ] Bookmark **tidak** ikut terhapus (regression check ke FR-3)
- [ ] Diverifikasi di device/emulator fisik

Kalau tab tertutup tapi cookie/cache masih ada di storage: **STATUS = NOT DONE**, walaupun secara visual terlihat "bersih".

### TASK: Implement FR-4 — Tracker Blocking (Data DuckDuckGo)

Task hanya DONE apabila:
- [ ] Blocklist DDG berhasil di-parse dan diterapkan di `shouldInterceptRequest`
- [ ] Dibuktikan: domain tracker yang ada di blocklist benar-benar tidak terkirim requestnya di halaman uji nyata
- [ ] Tidak ada false positive yang merusak fungsi situs first-party
- [ ] Versi/tanggal snapshot blocklist tercatat dan bisa diaudit
- [ ] Catatan lisensi NonCommercial (01_PRD.md Section 11) sudah direview status approval-nya sebelum masuk build rilis

### TASK: Implement FR-2 — Default Search DuckDuckGo

Task hanya DONE apabila:
- [ ] Input non-URL benar-benar diarahkan ke `duckduckgo.com`, dibuktikan lewat pengecekan URL akhir yang termuat
- [ ] Input URL valid tidak salah dikira sebagai search query
- [ ] Diverifikasi dengan beberapa jenis input (keyword, URL tanpa protokol, URL lengkap)

---

## 3. Mock ≠ Implementasi

Untuk requirement P0: mock/dummy tidak dianggap implementasi. Mock sementara wajib diberi label `// MOCK` di kode dan dicatat NOT DONE di tracker, tidak boleh di-merge sebagai representasi "selesai".

**Kegagalan yang harus dicegah:**

    Fire Button ditekan
       ↓
    Tab keliatan tertutup semua
       ↓
    Tapi cookie/cache masih nyangkut di storage
       ↓
    Agent melaporkan: "Fire Button sudah dibuat"
       ↓
    Kenyataan: data browsing masih bisa ditelusuri

Persis pola kegagalan "UI ada, fungsi tidak" yang jadi alasan dokumen-dokumen ini dibuat.

---

## 4. Hard Review — Pertanyaan Wajib

- Apakah fungsi ini benar-benar bekerja, atau cuma UI-nya ada?
- Apakah data (cookie/cache/history) benar-benar dihapus/disimpan, atau di-hardcode?
- Apakah blocklist benar-benar diterapkan di level request, atau cuma toggle kosong?
- Apakah error handling nyata?
- Apakah fitur masih bekerja setelah app restart?
- Apakah versi dependency sesuai 03_VERSION_MATRIX.md?
- Apakah implementasi sesuai 02_ARCHITECTURE.md?
- Apakah fitur sebelumnya masih bekerja (regression)?

**Agent tidak boleh jadi satu-satunya pihak yang menyatakan pekerjaannya selesai.**

---

## 5. Checkpoint & Kill Criteria

**Checkpoint 1 (Foundation):** Project build? Dependency kompatibel? App launch? Parsing blocklist DDG berhasil di skeleton project?
→ Kalau tidak: **STOP**.

**Checkpoint 2 (P0 pertama):** FR-2/FR-4/FR-6 benar-benar bekerja sesuai acceptance criteria, bukan cuma UI, tidak pakai mock tanpa label?
→ Kalau tidak: **STOP**, evaluasi ulang.

**Checkpoint 3 (semua P0 selesai):** Semua P0 di Section 5-6 (01_PRD.md) benar-benar DONE? Arsitektur masih sesuai kontrak? Ada yang mulai kerjain P1/P2 padahal P0 belum lolos?
→ Kalau buruk: **STOP / REPLAN**.

---

## 6. Prinsip Akhir

Progress diukur dari requirement yang benar-benar berfungsi dan terverifikasi, bukan jumlah kode. Kontrak di 4 dokumen ini (01_PRD, 02_ARCHITECTURE, 03_VERSION_MATRIX, 04_DEFINITION_OF_DONE) mengikat, terlepas seberapa kuat reasoning atau seberapa banyak kode yang ditulis agent.
