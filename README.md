# AdHunter

Auditor izin Android untuk menemukan aplikasi pelaku spam iklan, termasuk aplikasi yang tidak muncul di recents. Dipakai pribadi lewat sideload, bukan aplikasi Play Store.

## Fitur

### Audit Izin

- Memindai semua aplikasi terpasang menggunakan `QUERY_ALL_PACKAGES` agar app tanpa launcher dan app tersembunyi ikut terlihat.
- Mendeteksi empat izin berisiko: Overlay, Aksesibilitas, Admin perangkat, dan Listener notifikasi.
- Menampilkan ikon, nama app, nama paket, badge izin, sumber pemasangan (Play Store / sideload / lainnya), dan tanggal pasang.
- Skor risiko = jumlah izin berisiko + 1 jika sideload. Daftar diurutkan dari skor tertinggi.
- App sistem inti yang bertanda `FLAG_SYSTEM` dan tidak pernah diperbarui disembunyikan. Toggle "Tampilkan app sistem" membuka semuanya.
- Pemindaian berjalan lokal di perangkat (`Dispatchers.Default`), tanpa izin INTERNET dan tanpa pengiriman data.

### Pemantau aplikasi di layar

Pengganti `adb shell dumpsys window | findstr mCurrentFocus` yang berjalan langsung di HP.

- Foreground service bertipe `specialUse` dengan notifikasi permanen **AdHunter memantau**, tombol Hentikan di notifikasi, dan `START_STICKY` agar service dibuat ulang setelah proses aplikasi dimatikan.
- Mem-poll `UsageStatsManager.queryEvents` (`ACTIVITY_RESUMED` / `MOVE_TO_FOREGROUND`) setiap detik hanya saat layar menyala (`PowerManager.isInteractive`), lalu mencatat waktu, nama paket, nama aplikasi, dan nama activity bila tersedia.
- Log disimpan lokal di SQLite lewat `SQLiteOpenHelper`, maksimal **2000 entri terbaru**. Entri lama dibuang otomatis setiap kali entri baru masuk.
- Entri disorot merah bila paketnya punya izin berisiko menurut scanner Audit Izin, atau tidak punya launcher dan bukan app sistem. Alasan sorotan ditampilkan di kartu.
- Filter "Hanya disorot" dan tombol hapus log (dengan konfirmasi).
- Status layanan, tombol Mulai/Hentikan, serta kartu peringatan untuk Usage Access, izin notifikasi, dan optimasi baterai.

#### Batasan jujur

`UsageStats` hanya menangkap aplikasi/activity yang benar-benar tampil di layar sebagai activity. Iklan yang muncul sebagai overlay (`SYSTEM_ALERT_WINDOW`) **tidak tercatat** di layar Pemantau. Kasus seperti itu tetap ditangkap lewat badge **Overlay** di layar Audit Izin, karena overlay tidak selalu membuat activity baru. Gunakan kedua layar secara berdampingan untuk hasil terbaik.

## Izin yang perlu diaktifkan manual

1. **Usage Access** (wajib untuk Pemantau)
   Buka **Pengaturan → Aplikasi → Akses khusus → Akses penggunaan** (di sebagian perangkat: **Privasi → Akses khusus → Akses penggunaan**), pilih **AdHunter**, lalu aktifkan **Izinkan pelacakan penggunaan**. Alternatif cepat: tombol **Buka Usage Access** di layar Pemantau.
2. **Notifikasi** (Android 13 ke atas)
   Saat pemantauan pertama dimulai, Android menampilkan dialog izin notifikasi. Jika ditolak, aktifkan lewat **Pengaturan → Aplikasi → AdHunter → Notifikasi**. Pemantauan tetap berjalan walau notifikasi disembunyikan, tetapi status permanen tidak terlihat.
3. **Optimasi baterai** (disarankan)
   Tekan **Kecualikan** di kartu Optimasi baterai pada layar Pemantau. Jika dialog tidak tersedia, buka **Pengaturan → Baterai → Optimasi baterai → Semua aplikasi → AdHunter → Jangan optimalkan**.

Service sengaja dimulai dari tombol di aplikasi (bukan dari background) agar mematuhi batasan foreground service Android 14 ke atas. Pemantauan tidak berjalan otomatis setelah HP restart; mulai lagi dari layar Pemantau.

## Struktur folder

```
AdHunter/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/massbim/adhunter/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── AppAuditScanner.kt
│       │   │   ├── ForegroundLogStore.kt
│       │   │   ├── ForegroundMonitorService.kt
│       │   │   └── UsageAccess.kt
│       │   ├── model/
│       │   │   ├── AppAudit.kt
│       │   │   └── ForegroundLog.kt
│       │   └── ui/
│       │       ├── AppIcon.kt
│       │       ├── AppRoot.kt
│       │       ├── AuditScreen.kt
│       │       ├── AuditViewModel.kt
│       │       ├── MonitorScreen.kt
│       │       ├── MonitorViewModel.kt
│       │       ├── PermissionBadge.kt
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
