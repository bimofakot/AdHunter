# AdHunter

Auditor izin Android untuk menemukan aplikasi pelaku spam iklan, termasuk aplikasi yang tidak muncul di recents. Dipakai pribadi lewat sideload, bukan aplikasi Play Store.

## Cara kerja

- Memindai semua aplikasi terpasang menggunakan `QUERY_ALL_PACKAGES` agar app tanpa launcher dan app tersembunyi ikut terlihat.
- Mendeteksi empat izin berisiko: Overlay, Aksesibilitas, Admin perangkat, dan Listener notifikasi.
- Menampilkan ikon, nama app, nama paket, badge izin, sumber pemasangan (Play Store / sideload / lainnya), dan tanggal pasang.
- Skor risiko = jumlah izin berisiko + 1 jika sideload. Daftar diurutkan dari skor tertinggi.
- App sistem inti yang bertanda `FLAG_SYSTEM` dan tidak pernah diperbarui disembunyikan. Toggle "Tampilkan app sistem" membuka semuanya.
- Pemindaian berjalan lokal di perangkat (`Dispatchers.Default`), tanpa izin INTERNET dan tanpa pengiriman data.

## Struktur folder

```
AdHunter/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/massbim/adhunter/
│       │   ├── MainActivity.kt
│       │   ├── data/AppAuditScanner.kt
│       │   ├── model/AppAudit.kt
│       │   └── ui/
│       │       ├── AuditScreen.kt
│       │       ├── AuditViewModel.kt
│       │       └── theme/Theme.kt
│       └── res/
│           ├── drawable/
│           ├── mipmap-anydpi/
│           ├── mipmap-anydpi-v26/
│           ├── values/
│           └── values-night/
├── .github/workflows/build-apk.yml
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

## Unduh APK dari GitHub Actions

1. Buka [Actions AdHunter](https://github.com/bimofakot/AdHunter/actions).
2. Pilih run terbaru workflow **build-apk** dengan tanda centang hijau.
3. Gulir ke bagian **Artifacts**, lalu unduh **AdHunter-debug.zip**.
4. Ekstrak zip dan pasang `app-debug.apk`. Aktifkan "Install unknown apps" untuk file manager atau browser yang dipakai.

Artifact disimpan 30 hari sejak build. Build baru bisa dijalankan lewat tombol **Run workflow** (`workflow_dispatch`). Pastikan sudah login ke akun GitHub yang punya akses ke repo.

## Build lokal

Kebutuhan: JDK 17 dan Android SDK Platform 37.

```bash
./gradlew assembleDebug
```

APK debug ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Versi build

- Gradle 9.6.0
- Android Gradle Plugin 9.5.1
- Kotlin 2.4.10
- Compose BOM 2026.09.00
- minSdk 26, compileSdk 37, targetSdk 37
