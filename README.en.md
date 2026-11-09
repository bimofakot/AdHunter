# AdHunter

**Language:** [Bahasa Indonesia](README.md) · English

[![build-apk](https://github.com/bimofakot/AdHunter/actions/workflows/build-apk.yml/badge.svg?branch=main)](https://github.com/bimofakot/AdHunter/actions/workflows/build-apk.yml)
[![latest release](https://img.shields.io/github/v/release/bimofakot/AdHunter?label=release&color=3DDC84)](https://github.com/bimofakot/AdHunter/releases/latest)
[![license](https://img.shields.io/github/license/bimofakot/AdHunter)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)

AdHunter helps you find the Android apps that quietly show ads: it scans installed apps for risky permissions, then logs every app that comes to the foreground with a timestamped history. Everything runs locally on the phone, with no `INTERNET` permission and no data upload.

This app is for personal sideloading, not for distribution on the Play Store. The interface is currently in Indonesian.

## Download and install

The latest APK is published on the **[Releases](https://github.com/bimofakot/AdHunter/releases/latest)** page.

1. Open the latest release and download the `AdHunter-v….apk` file under **Assets**.
2. Open the APK. If prompted, allow **Install unknown apps** for the browser or file manager you use.
3. Install it, open AdHunter, and grant the permissions described below.

Requires **Android 8.0 (API 26)** or newer.

- **Updating:** install the new APK over the old one; no uninstall is needed. Every release is signed with the same key, so settings and the Monitor log are preserved.
- **One-time step:** if you previously installed an APK from a GitHub Actions artifact (debug-signed), uninstall it before installing the first release.

## Features

### Permission audit

- Scans every installed app using `QUERY_ALL_PACKAGES`, so apps without a launcher and hidden apps are included.
- Detects four risky permissions: Overlay, Accessibility, Device admin, and Notification listener.
- Shows the app icon, name, package name, permission badges, install source (Play Store / sideload / other), and install date.
- Risk score = number of risky permissions + 1 for sideloaded apps. The list is sorted by score.
- Core system apps flagged `FLAG_SYSTEM` that have never been updated are hidden. The "Show system apps" toggle reveals all of them.
- Scanning runs locally on the device (`Dispatchers.Default`).

### Foreground app monitor

A replacement for `adb shell dumpsys window | findstr mCurrentFocus` that runs on the phone itself.

- A `specialUse` foreground service with a persistent **AdHunter is monitoring** notification, a Stop action in the notification, and `START_STICKY` so the service is recreated if the process is killed.
- Polls `UsageStatsManager.queryEvents` (`ACTIVITY_RESUMED` / `MOVE_TO_FOREGROUND`) once per second only while the screen is on (`PowerManager.isInteractive`), recording the time, package name, app name, and activity name when available.
- Logs are stored locally in SQLite via `SQLiteOpenHelper`, capped at the **latest 2000 entries**. Older entries are dropped whenever a new one arrives.
- Entries are highlighted in red when the package has a risky permission per the permission scan, or has no launcher and is not a system app. The reason is shown on the card.
- "Only highlighted" filter and a clear-log button (with confirmation).
- Service status, Start/Stop button, and warning cards for Usage Access, notification permission, and battery optimization.

#### Honest limitations

`UsageStats` only captures apps/activities that actually appear on screen as activities. Ads rendered as overlays (`SYSTEM_ALERT_WINDOW`) are **not** logged in the Monitor screen. Those cases are still caught by the **Overlay** badge on the Permission audit screen, because overlays do not always create a new activity. Use both screens together for the best results.

## Permissions to enable manually

1. **Usage Access** (required by the Monitor)
   Open **Settings → Apps → Special app access → Usage access** (on some devices: **Privacy → Special app access → Usage access**), select **AdHunter**, then enable **Permit usage tracking**. Shortcut: the **Open Usage Access** button on the Monitor screen.
2. **Notifications** (Android 13 and newer)
   Android shows the notification permission dialog when monitoring first starts. If you decline, enable it via **Settings → Apps → AdHunter → Notifications**. Monitoring still runs with hidden notifications, but the persistent status will not be visible.
3. **Battery optimization** (recommended)
   Tap **Exclude** on the Battery optimization card in the Monitor screen. If the dialog is unavailable, open **Settings → Battery → Battery optimization → All apps → AdHunter → Don't optimize**.

The service is deliberately started from a button in the app (not from the background) to comply with Android 14+ foreground service restrictions. Monitoring does not resume automatically after a phone restart; start it again from the Monitor screen.

## Privacy

- No `INTERNET` permission: scan results and logs cannot leave the device.
- `QUERY_ALL_PACKAGES` is only used to read the list of installed packages, including apps without a launcher and hidden apps.
- Monitor logs are stored in the app's private SQLite database, capped at 2000 entries, and can be cleared from the Monitor screen.
- No analytics, crash reporting, or third-party services.

## Automated releases

Every push to `main` — including merged pull requests — runs the [`release`](.github/workflows/release.yml) workflow:

1. Builds `assembleRelease` with the release key from GitHub Secrets.
2. `versionCode` follows the workflow run number (always increasing); `versionName` is `0.1.0-build<N>`.
3. Creates tag `v0.1.0-build<N>` and a GitHub Release containing the signed APK.

Required secrets: `ADHUNTER_KEYSTORE_BASE64`, `ADHUNTER_KEYSTORE_PASSWORD`, `ADHUNTER_KEY_ALIAS`, `ADHUNTER_KEY_PASSWORD`. Until the secrets are set, the release job is skipped and the [`build-apk`](.github/workflows/build-apk.yml) workflow still verifies the build on every branch.

## Build from source

Requirements: JDK 17, Android SDK Platform 37, and build-tools 37.0.0.

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

For a signed release build, provide a `keystore.properties` file in the project root (already git-ignored):

```properties
storeFile=/path/to/adhunter-release.keystore
storePassword=…
keyAlias=adhunter
keyPassword=…
```

Then run `./gradlew assembleRelease`. When this file exists, debug builds use the same key too, so local builds and release APKs can update each other without signature conflicts. Without it, debug builds use the default debug key.

## Project structure

```
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

- Kotlin 2.4.10 with Jetpack Compose (Material 3)
- Gradle 9.6.0, Android Gradle Plugin 9.5.1, Compose BOM 2026.09.00
- minSdk 26, compileSdk 37, targetSdk 37
- `specialUse` foreground service, `UsageStatsManager`, SQLite via `SQLiteOpenHelper`

## License

[MIT](LICENSE) © 2026 Bimofakot
