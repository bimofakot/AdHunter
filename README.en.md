<div align="right">

**English** · [Bahasa Indonesia](README.md)

</div>

# AdHunter

**Permission scanner and foreground app monitor for hunting rogue ads on Android.** All data stays on the phone: no internet permission, no analytics, no third-party services.

[![build-apk](https://github.com/bimofakot/AdHunter/actions/workflows/build-apk.yml/badge.svg?branch=main)](https://github.com/bimofakot/AdHunter/actions/workflows/build-apk.yml)
[![latest release](https://img.shields.io/github/v/release/bimofakot/AdHunter?label=release&color=3DDC84)](https://github.com/bimofakot/AdHunter/releases/latest)
[![license](https://img.shields.io/github/license/bimofakot/AdHunter)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)

This app is for personal sideloading, not for Play Store distribution. The interface is currently in Indonesian.

## Table of contents

- [Download and install](#download-and-install)
- [Features](#features)
- [Permissions to enable manually](#permissions-to-enable-manually)
- [Privacy](#privacy)
- [Automated releases](#automated-releases)
- [Build from source](#build-from-source)
- [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [License](#license)

## Download and install

The latest APK is published on the **[Releases](https://github.com/bimofakot/AdHunter/releases/latest)** page.

1. Open the latest release and download `AdHunter-v….apk` under **Assets**.
2. Open the APK. If prompted, allow **Install unknown apps** for the browser or file manager you use.
3. Install it, open AdHunter, and grant the permissions described below.

**Requires:** Android 8.0 (API 26) or newer.

- **Updating:** install the new APK over the old one; no uninstall is needed. Every release uses the same key, so settings and the Monitor log are preserved.
- **One-time step:** if you previously installed an APK from a GitHub Actions artifact (debug-signed), uninstall it before installing the first release.
- **Verify (optional):** also download the `.apk.sha256` file, then run `sha256sum -c AdHunter-v….apk.sha256` in the same folder.

## Features

### Permission audit

The four permissions most often abused by adware:

| Permission | What makes it risky |
| --- | --- |
| Overlay (`SYSTEM_ALERT_WINDOW`) | Draws ads on top of other apps. |
| Accessibility | Reads screen content and taps buttons automatically. |
| Device admin | Makes uninstalling harder and can lock the device. |
| Notification listener | Reads every notification, including OTP codes. |

- Scans every installed app via `QUERY_ALL_PACKAGES`, including apps without a launcher and hidden apps.
- Each card shows the icon, app name, package name, permission badges, install source (Play Store / sideload / other), and install date.
- Risk score = number of risky permissions + 1 for sideloaded apps; the list is sorted by score.
- Core system apps (`FLAG_SYSTEM` that have never been updated) stay hidden until the "Show system apps" toggle is on.
- Scanning runs locally on the device (`Dispatchers.Default`).

### Foreground app monitor

A replacement for `adb shell dumpsys window | findstr mCurrentFocus` that runs on the phone itself.

- **Service:** a `specialUse` foreground service with a persistent *AdHunter is monitoring* notification, a Stop action, and `START_STICKY`.
- **Capture:** polls `UsageStatsManager.queryEvents` once per second while the screen is on, storing the time, package, app name, and activity.
- **Storage:** local SQLite, capped at the latest 2000 entries; older entries are dropped automatically.
- **Highlighting:** red cards for packages with risky permissions or without a launcher that are not system apps, including the reason.
- **Controls:** "Only highlighted" filter, clear-log with confirmation, service status, Start/Stop button, and permission warning cards.

> [!IMPORTANT]
> **Limitation:** `UsageStats` only captures apps or activities that appear on screen as activities. Overlay ads (`SYSTEM_ALERT_WINDOW`) are not logged by the Monitor; those cases are caught by the **Overlay** badge on the Audit screen. Use both screens together.

## Permissions to enable manually

### 1. Usage Access (required by the Monitor)

Open **Settings → Apps → Special app access → Usage access** (on some devices: **Privacy → Special app access → Usage access**), select **AdHunter**, then enable **Permit usage tracking**. Shortcut: the **Open Usage Access** button on the Monitor screen.

### 2. Notifications (Android 13 and newer)

The permission dialog appears when monitoring first starts. If you decline, enable it via **Settings → Apps → AdHunter → Notifications**. Monitoring still runs without notifications, but the persistent status will not be visible.

### 3. Battery optimization (recommended)

Tap **Exclude** on the Battery optimization card in the Monitor screen. If the dialog is unavailable: **Settings → Battery → Battery optimization → All apps → AdHunter → Don't optimize**.

The service is deliberately started from a button in the app, not from the background, to comply with Android 14+ foreground service restrictions. Monitoring does not resume automatically after a phone restart; start it again from the Monitor screen.

## Privacy

- No `INTERNET` permission: scan results and logs cannot leave the device.
- `QUERY_ALL_PACKAGES` is only used to list installed packages, including apps without a launcher and hidden apps.
- Monitor logs are stored in the app's private SQLite database (max 2000 entries) and can be cleared from the Monitor screen.
- No analytics, crash reporting, or third-party services.

## Automated releases

Every push to `main` — including merged pull requests — runs the [`release`](.github/workflows/release.yml) workflow:

1. Builds `assembleRelease` with the release key from GitHub Secrets.
2. `versionCode` follows the workflow run number; `versionName` is `0.1.0-build<N>`.
3. Creates tag `v0.1.0-build<N>` and a GitHub Release containing the signed APK and its `.sha256` checksum.

Required secrets: `ADHUNTER_KEYSTORE_BASE64`, `ADHUNTER_KEYSTORE_PASSWORD`, `ADHUNTER_KEY_ALIAS`, `ADHUNTER_KEY_PASSWORD`. Until the secrets are set, the release job is skipped and the [`build-apk`](.github/workflows/build-apk.yml) workflow still verifies the build on every branch.

## Build from source

Requirements: JDK 17, Android SDK Platform 37, and build-tools 37.0.0.

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Signed release build

Provide a `keystore.properties` file in the project root (already git-ignored):

```properties
storeFile=/path/to/adhunter-release.keystore
storePassword=…
keyAlias=adhunter
keyPassword=…
```

Then run `./gradlew assembleRelease`. When this file exists, debug builds use the same key too, so local builds and release APKs can update each other without signature conflicts. Without it, debug builds use the default debug key.

## Project structure

```text
AdHunter/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/massbim/adhunter/
│   │   ├── data/    # permission scanner, monitor service, SQLite log store, Usage Access check
│   │   ├── model/   # AppAudit, ForegroundLog
│   │   └── ui/      # Audit and Monitor screens, ViewModels, theme
│   └── res/
├── .github/workflows/   # build-apk.yml, release.yml
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew, gradlew.bat
```

## Tech stack

| Component | Version / notes |
| --- | --- |
| Kotlin | 2.4.10 |
| Jetpack Compose | BOM 2026.09.00 (Material 3) |
| Gradle | 9.6.0 |
| Android Gradle Plugin | 9.5.1 |
| SDK | min 26, compile 37, target 37 |
| Local storage | SQLite (`SQLiteOpenHelper`) |
| Monitoring | `UsageStatsManager`, `specialUse` foreground service |

## License

[MIT](LICENSE) © 2026 Bimofakot
