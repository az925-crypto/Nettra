# 03_VERSION_MATRIX.md

## Tech Stack / Version Contract: [Nama Aplikasi — TBD]

**Versi:** 1.0
**Tanggal cross-check:** 27 Agustus 2026
**Status:** Draft — beberapa baris ditandai **PERLU KONFIRMASI**.

> **Aturan mengikat:** Agent tidak boleh upgrade/downgrade versi secara diam-diam. Dependency butuh versi lain → **STOP → laporkan konflik → jelaskan dampak → usulkan alternatif → tunggu approval.**

---

## 1. Build Tools

| Komponen | Versi | Status |
|---|---|---|
| JDK | 21 (LTS) | Rekomendasi |
| Gradle | 9.5.1 | Terverifikasi stabil (rilis 12 Mei 2026) |
| Android Gradle Plugin (AGP) | 8.x stabil terbaru, atau 9.x kalau sudah rilis stabil | **PERLU KONFIRMASI** — cek `https://developer.android.com/build/releases/gradle-plugin` |
| Kotlin | 2.3.20 | Terverifikasi (rilis ~Maret 2026, dukung Gradle 9.3.0+) |

---

## 2. UI Framework

| Komponen | Versi | Status |
|---|---|---|
| Jetpack Compose (UI core) | Line 1.10.x | Terverifikasi arah versinya |
| Compose BOM | — | **PERLU KONFIRMASI** — pakai BOM terbaru saat lock-in, jangan pin manual per-artifact. Cek `https://developer.android.com/develop/ui/compose/bom/bom-mapping` |

---

## 3. Android SDK

| Komponen | Versi | Status |
|---|---|---|
| compileSdk | 36 (Android 16) | Terverifikasi |
| targetSdk | 36 | Cek requirement Play Console terkini sebelum submit |
| minSdk | 26 (Android 8.0) | Konsisten dengan project lain di ekosistem ini |

---

## 4. AndroidX & Data

| Komponen | Versi | Status |
|---|---|---|
| AndroidX Core/Activity/Lifecycle | Versi stabil terbaru saat lock-in | **PERLU KONFIRMASI** |
| Room (untuk `core-tabs`) | Versi stabil terbaru, kompatibel Kotlin 2.3.20 (KSP2) | **PERLU KONFIRMASI** |
| Format data blocklist DDG | JSON, sesuai skema `duckduckgo/tracker-blocklists` (`web`) | Terverifikasi strukturnya via README repo — validasi parsing di Fase 0 |

---

## 5. Testing

| Komponen | Versi | Status |
|---|---|---|
| JUnit4 | 4.13.2 | Stabil |
| AndroidX Test | Versi stabil terbaru saat lock-in | **PERLU KONFIRMASI** |

---

## 6. Item Wajib Dikonfirmasi Sebelum Development Skala Besar

1. Versi AGP final
2. Compose BOM exact version
3. Versi AndroidX Core/Activity/Lifecycle/Room exact
4. **Lisensi pemakaian komersial data Tracker Radar/blocklist DuckDuckGo** (lihat 01_PRD.md Section 11) — ini bukan cuma soal versi teknis, tapi syarat legal sebelum blocklist DDG dipakai di build rilis kalau app dimonetisasi

**Cara konfirmasi versi teknis:** `./gradlew dependencies` di project skeleton kosong, pastikan tidak ada conflict resolution warning.
