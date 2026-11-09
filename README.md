<div align="right">

[Bahasa Indonesia](README.md) · **English**

</div>

# AdHunter

**Pemindai izin dan pemantau aplikasi untuk memburu iklan liar di Android.** Semua data tetap di HP: tanpa izin internet, tanpa analytics, tanpa layanan pihak ketiga.

[![build-apk](https://github.com/bimofakot/AdHunter/actions/workflows/build-apk.yml/badge.svg?branch=main)](https://github.com/bimofakot/AdHunter/actions/workflows/build-apk.yml)
[![rilis terbaru](https://img.shields.io/github/v/release/bimofakot/AdHunter?label=rilis&color=3DDC84)](https://github.com/bimofakot/AdHunter/releases/latest)
[![lisensi](https://img.shields.io/github/license/bimofakot/AdHunter)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)

Aplikasi ini untuk pemakaian pribadi lewat sideload, bukan untuk Play Store.

## Daftar isi

- [Unduh dan pasang](#unduh-dan-pasang)
- [Fitur](#fitur)
- [Izin yang perlu diaktifkan manual](#izin-yang-perlu-diaktifkan-manual)
- [Privasi](#privasi)
- [Rilis otomatis](#rilis-otomatis)
- [Build dari source](#build-dari-source)
- [Struktur project](#struktur-project)
- [Teknologi](#teknologi)
- [Lisensi](#lisensi)

## Unduh dan pasang

APK terbaru ada di halaman **[Releases](https://github.com/bimofakot/AdHunter/releases/latest)**.

1. Buka rilis terbaru, lalu unduh `AdHunter-v….apk` di bagian **Assets**.
2. Buka file APK-nya. Jika diminta, aktifkan **Instal aplikasi tak dikenal** untuk browser atau file manager yang dipakai.
3. Pasang, buka AdHunter, lalu aktifkan izin yang diminta.

**Syarat:** Android 8.0 (API 26) ke atas.

- **Update:** pasang APK baru langsung di atas versi lama, tanpa uninstall. Semua rilis memakai kunci yang sama, jadi pengaturan dan log Pemantau tetap aman.
- **Sekali saja:** jika sebelumnya memakai APK dari artifact GitHub Actions (tanda tangan debug), uninstall dulu sebelum memasang rilis pertama.
- **Verifikasi (opsional):** unduh juga `.apk.sha256`, lalu jalankan `sha256sum -c AdHunter-v….apk.sha256` di folder yang sama.

## Fitur

### Audit izin

Empat izin yang paling sering dipakai adware:

| Izin | Kemampuan yang membuatnya berisiko |
| --- | --- |
| Overlay (`SYSTEM_ALERT_WINDOW`) | Menampilkan iklan di atas aplikasi lain. |
| Aksesibilitas | Membaca isi layar dan menekan tombol secara otomatis. |
| Admin perangkat | Mempersulit uninstall dan bisa mengunci perangkat. |
| Listener notifikasi | Membaca semua notifikasi, termasuk kode OTP. |

- Memindai semua aplikasi terpasang lewat `QUERY_ALL_PACKAGES`, termasuk aplikasi tanpa launcher dan yang disembunyikan.
- Setiap kartu menampilkan ikon, nama, paket, badge izin, sumber pemasangan (Play Store / sideload / lainnya), dan tanggal pasang.
- Skor risiko = jumlah izin berisiko + 1 untuk sideload; daftar diurutkan dari skor tertinggi.
- Aplikasi sistem inti (`FLAG_SYSTEM` yang tidak pernah diperbarui) disembunyikan sampai toggle "Tampilkan app sistem" dinyalakan.
- Pemindaian berjalan lokal di perangkat (`Dispatchers.Default`).

### Pemantau aplikasi di layar

Pengganti `adb shell dumpsys window | findstr mCurrentFocus` yang jalan langsung di HP.

- **Service:** foreground service `specialUse` dengan notifikasi permanen *AdHunter memantau*, tombol Hentikan, dan `START_STICKY`.
- **Pencatatan:** poll `UsageStatsManager.queryEvents` setiap detik saat layar menyala, lalu simpan waktu, paket, nama aplikasi, dan activity.
- **Penyimpanan:** SQLite lokal, maksimal 2000 entri terbaru; entri lama dibuang otomatis.
- **Sorotan:** kartu merah untuk paket berizin berisiko atau tanpa launcher yang bukan aplikasi sistem, lengkap dengan alasannya.
- **Kontrol:** filter "Hanya disorot", hapus log dengan konfirmasi, status layanan, tombol Mulai/Hentikan, serta kartu peringatan izin.

> [!IMPORTANT]
> **Batasan:** `UsageStats` hanya menangkap aplikasi atau activity yang tampil sebagai activity. Iklan overlay (`SYSTEM_ALERT_WINDOW`) tidak tercatat di Pemantau; kasus itu tertangkap lewat badge **Overlay** di layar Audit. Pakai kedua layar secara berdampingan.

## Izin yang perlu diaktifkan manual

### 1. Usage Access (wajib untuk Pemantau)

Buka **Pengaturan → Aplikasi → Akses khusus → Akses penggunaan** (di sebagian perangkat: **Privasi → Akses khusus → Akses penggunaan**), pilih **AdHunter**, lalu aktifkan **Izinkan pelacakan penggunaan**. Alternatif cepat: tombol **Buka Usage Access** di layar Pemantau.

### 2. Notifikasi (Android 13 ke atas)

Dialog izin muncul saat pemantauan pertama dimulai. Jika ditolak, aktifkan lewat **Pengaturan → Aplikasi → AdHunter → Notifikasi**. Pemantauan tetap berjalan tanpa notifikasi, tetapi status permanen tidak terlihat.

### 3. Optimasi baterai (disarankan)

Tekan **Kecualikan** di kartu Optimasi baterai pada layar Pemantau. Jika dialog tidak tersedia: **Pengaturan → Baterai → Optimasi baterai → Semua aplikasi → AdHunter → Jangan optimalkan**.

Service sengaja dimulai dari tombol di aplikasi, bukan dari background, agar sesuai batasan foreground service Android 14 ke atas. Pemantauan tidak berjalan otomatis setelah HP restart; mulai lagi dari layar Pemantau.

## Privasi

- Tanpa izin `INTERNET`: hasil pemindaian dan log tidak bisa keluar dari perangkat.
- `QUERY_ALL_PACKAGES` hanya dipakai untuk membaca daftar paket terpasang, termasuk aplikasi tanpa launcher dan aplikasi tersembunyi.
- Log Pemantau tersimpan di SQLite privat aplikasi (maksimal 2000 entri) dan bisa dihapus dari layar Pemantau.
- Tanpa analytics, crash reporting, atau layanan pihak ketiga.

## Rilis otomatis

Setiap push ke `main` — termasuk hasil merge — menjalankan workflow [`release`](.github/workflows/release.yml):

1. Build `assembleRelease` dengan kunci rilis dari GitHub Secrets.
2. `versionCode` naik mengikuti nomor run; `versionName` berbentuk `0.1.0-build<N>`.
3. Terbit tag `v0.1.0-build<N>` dan GitHub Release berisi APK bertanda tangan beserta checksum `.sha256`.

Secret yang dibutuhkan: `ADHUNTER_KEYSTORE_BASE64`, `ADHUNTER_KEYSTORE_PASSWORD`, `ADHUNTER_KEY_ALIAS`, `ADHUNTER_KEY_PASSWORD`. Tanpa secret, job rilis dilewati dan workflow [`build-apk`](.github/workflows/build-apk.yml) tetap memeriksa build di setiap branch.

## Build dari source

Kebutuhan: JDK 17, Android SDK Platform 37, dan build-tools 37.0.0.

```bash
./gradlew assembleDebug
```

APK debug ada di `app/build/outputs/apk/debug/app-debug.apk`.

### Build rilis bertanda tangan

Sediakan `keystore.properties` di root project (sudah diabaikan git):

```properties
storeFile=/path/to/adhunter-release.keystore
storePassword=…
keyAlias=adhunter
keyPassword=…
```

Lalu jalankan `./gradlew assembleRelease`. Bila file ini ada, build debug ikut memakai kunci yang sama sehingga build lokal dan APK rilis bisa saling menimpa tanpa bentrok tanda tangan. Tanpa file ini, debug memakai kunci debug bawaan.

## Struktur project

```text
AdHunter/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/massbim/adhunter/
│   │   ├── data/    # pemindai izin, service pemantau, penyimpanan log SQLite, cek Usage Access
│   │   ├── model/   # AppAudit, ForegroundLog
│   │   └── ui/      # layar Audit dan Pemantau, ViewModel, tema
│   └── res/
├── .github/workflows/   # build-apk.yml, release.yml
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew, gradlew.bat
```

## Teknologi

| Komponen | Versi / keterangan |
| --- | --- |
| Kotlin | 2.4.10 |
| Jetpack Compose | BOM 2026.09.00 (Material 3) |
| Gradle | 9.6.0 |
| Android Gradle Plugin | 9.5.1 |
| SDK | min 26, compile 37, target 37 |
| Penyimpanan lokal | SQLite (`SQLiteOpenHelper`) |
| Pemantauan | `UsageStatsManager`, foreground service `specialUse` |

## Lisensi

[MIT](LICENSE) © 2026 Bimofakot
