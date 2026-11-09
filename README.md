# AdHunter

**Bahasa:** Bahasa Indonesia · [English](README.en.md)

[![build-apk](https://github.com/bimofakot/AdHunter/actions/workflows/build-apk.yml/badge.svg?branch=main)](https://github.com/bimofakot/AdHunter/actions/workflows/build-apk.yml)
[![rilis terbaru](https://img.shields.io/github/v/release/bimofakot/AdHunter?label=rilis&color=3DDC84)](https://github.com/bimofakot/AdHunter/releases/latest)
[![lisensi](https://img.shields.io/github/license/bimofakot/AdHunter)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)

AdHunter membantu menemukan aplikasi Android yang diam-diam memunculkan iklan: memindai izin berisiko dari semua aplikasi terpasang, lalu mencatat aplikasi yang tampil di layar beserta riwayatnya. Semua proses berjalan lokal di HP, tanpa izin `INTERNET` dan tanpa pengiriman data.

Aplikasi ini untuk pemakaian pribadi lewat sideload, bukan untuk Play Store.

## Unduh dan pasang

APK terbaru tersedia di halaman **[Releases](https://github.com/bimofakot/AdHunter/releases/latest)**.

1. Buka halaman rilis terbaru, lalu unduh file `AdHunter-v….apk` di bagian **Assets**.
2. Buka file APK tersebut. Jika diminta, aktifkan **Instal aplikasi tak dikenal** untuk browser atau file manager yang dipakai.
3. Pasang, buka AdHunter, lalu aktifkan izin yang diminta (lihat bagian berikut).

Syarat perangkat: **Android 8.0 (API 26)** ke atas.

- **Update:** pasang APK baru langsung di atas versi lama, tidak perlu uninstall. Semua rilis ditandatangani dengan kunci yang sama, sehingga pengaturan dan log Pemantau tetap tersimpan.
- **Sekali saja:** jika sebelumnya Anda memasang APK dari artifact GitHub Actions (tanda tangan debug), uninstall dulu sebelum memasang rilis pertama.

## Fitur

### Audit izin

- Memindai semua aplikasi terpasang menggunakan `QUERY_ALL_PACKAGES` agar aplikasi tanpa launcher dan aplikasi tersembunyi ikut terlihat.
- Mendeteksi empat izin berisiko: Overlay, Aksesibilitas, Admin perangkat, dan Listener notifikasi.
- Menampilkan ikon, nama aplikasi, nama paket, badge izin, sumber pemasangan (Play Store / sideload / lainnya), dan tanggal pasang.
- Skor risiko = jumlah izin berisiko + 1 jika sideload. Daftar diurutkan dari skor tertinggi.
- Aplikasi sistem inti yang bertanda `FLAG_SYSTEM` dan tidak pernah diperbarui disembunyikan. Toggle "Tampilkan app sistem" membuka semuanya.
- Pemindaian berjalan lokal di perangkat (`Dispatchers.Default`).

### Pemantau aplikasi di layar

Pengganti `adb shell dumpsys window | findstr mCurrentFocus` yang berjalan langsung di HP.

- Foreground service bertipe `specialUse` dengan notifikasi permanen **AdHunter memantau**, tombol Hentikan di notifikasi, dan `START_STICKY` agar service dibuat ulang setelah proses aplikasi dimatikan.
- Mem-poll `UsageStatsManager.queryEvents` (`ACTIVITY_RESUMED` / `MOVE_TO_FOREGROUND`) setiap detik hanya saat layar menyala (`PowerManager.isInteractive`), lalu mencatat waktu, nama paket, nama aplikasi, dan nama activity bila tersedia.
- Log disimpan lokal di SQLite lewat `SQLiteOpenHelper`, maksimal **2000 entri terbaru**. Entri lama dibuang otomatis setiap kali entri baru masuk.
- Entri disorot merah bila paketnya punya izin berisiko menurut pemindai izin, atau tidak punya launcher dan bukan aplikasi sistem. Alasan sorotan ditampilkan di kartu.
- Filter "Hanya disorot" dan tombol hapus log (dengan konfirmasi).
- Status layanan, tombol Mulai/Hentikan, serta kartu peringatan untuk Usage Access, izin notifikasi, dan optimasi baterai.

#### Batasan jujur

`UsageStats` hanya menangkap aplikasi/activity yang benar-benar tampil di layar sebagai activity. Iklan yang muncul sebagai overlay (`SYSTEM_ALERT_WINDOW`) **tidak tercatat** di layar Pemantau. Kasus seperti itu tetap tertangkap lewat badge **Overlay** di layar Audit izin, karena overlay tidak selalu membuat activity baru. Gunakan kedua layar secara berdampingan untuk hasil terbaik.

## Izin yang perlu diaktifkan manual

1. **Usage Access** (wajib untuk Pemantau)
   Buka **Pengaturan → Aplikasi → Akses khusus → Akses penggunaan** (di sebagian perangkat: **Privasi → Akses khusus → Akses penggunaan**), pilih **AdHunter**, lalu aktifkan **Izinkan pelacakan penggunaan**. Alternatif cepat: tombol **Buka Usage Access** di layar Pemantau.
2. **Notifikasi** (Android 13 ke atas)
   Saat pemantauan pertama dimulai, Android menampilkan dialog izin notifikasi. Jika ditolak, aktifkan lewat **Pengaturan → Aplikasi → AdHunter → Notifikasi**. Pemantauan tetap berjalan walau notifikasi disembunyikan, tetapi status permanen tidak terlihat.
3. **Optimasi baterai** (disarankan)
   Tekan **Kecualikan** di kartu Optimasi baterai pada layar Pemantau. Jika dialog tidak tersedia, buka **Pengaturan → Baterai → Optimasi baterai → Semua aplikasi → AdHunter → Jangan optimalkan**.

Service sengaja dimulai dari tombol di aplikasi (bukan dari background) agar mematuhi batasan foreground service Android 14 ke atas. Pemantauan tidak berjalan otomatis setelah HP restart; mulai lagi dari layar Pemantau.

## Privasi

- Tidak ada izin `INTERNET`: hasil pemindaian dan log tidak bisa keluar dari perangkat.
- `QUERY_ALL_PACKAGES` hanya dipakai untuk membaca daftar paket terpasang, termasuk aplikasi tanpa launcher dan aplikasi tersembunyi.
- Log Pemantau disimpan di SQLite privat aplikasi, maksimal 2000 entri, dan bisa dihapus dari layar Pemantau.
- Tidak ada analytics, crash reporting, atau layanan pihak ketiga.

## Rilis otomatis

Setiap push ke `main` — termasuk hasil merge pull request — menjalankan workflow [`release`](.github/workflows/release.yml):

1. Build `assembleRelease` dengan kunci rilis dari GitHub Secrets.
2. `versionCode` mengikuti nomor run (selalu naik), `versionName` berbentuk `0.1.0-build<N>`.
3. Tag `v0.1.0-build<N>` dan GitHub Release berisi APK bertanda tangan.

Secret yang diperlukan: `ADHUNTER_KEYSTORE_BASE64`, `ADHUNTER_KEYSTORE_PASSWORD`, `ADHUNTER_KEY_ALIAS`, `ADHUNTER_KEY_PASSWORD`. Selama secret belum diset, job rilis dilewati dan workflow [`build-apk`](.github/workflows/build-apk.yml) tetap memeriksa build di setiap branch.

## Build dari source

Kebutuhan: JDK 17, Android SDK Platform 37, dan build-tools 37.0.0.

```bash
./gradlew assembleDebug
```

APK debug ada di `app/build/outputs/apk/debug/app-debug.apk`.

Untuk build rilis bertanda tangan, sediakan `keystore.properties` di root project (sudah diabaikan git):

```properties
storeFile=/path/ke-adhunter-release.keystore
storePassword=…
keyAlias=adhunter
keyPassword=…
```

Lalu jalankan `./gradlew assembleRelease`. Bila file ini ada, build debug juga memakai kunci yang sama sehingga build lokal dan APK rilis bisa saling menimpa tanpa bentrok tanda tangan. Tanpa file ini, build debug memakai kunci debug bawaan.

## Struktur project

```
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

- Kotlin 2.4.10 dengan Jetpack Compose (Material 3)
- Gradle 9.6.0, Android Gradle Plugin 9.5.1, Compose BOM 2026.09.00
- minSdk 26, compileSdk 37, targetSdk 37
- Foreground service `specialUse`, `UsageStatsManager`, SQLite lewat `SQLiteOpenHelper`

## Lisensi

[MIT](LICENSE) © 2026 Bimofakot
