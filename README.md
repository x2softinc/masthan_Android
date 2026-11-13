# Masthan Gani Masjid - Android

Kotlin / Jetpack Compose port of the iOS app in [`x2softinc/Masthan`](https://github.com/x2softinc/Masthan).
See [SYNC.md](SYNC.md) for how the two repos are kept in step and the Swift -> Kotlin file map.

Features: daily Adhan/Iqama times (calculated locally with PrayTimes + masjid schedule from the Masjid Pro
API), 16-day pager, Qibla compass, masjid details, Adhan / Iqama notifications with selectable Adhan sound,
settings, legal documents and a forced-update gate.

## Build

Requirements: JDK 17+, Android SDK (API 35). Create `local.properties` (git-ignored):

```properties
sdk.dir=/path/to/Android/sdk
# API key for the app-version check (iOS keeps it in Info.plist as VERSION_API_KEY)
VERSION_API_KEY=...
```

```bash
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug
./gradlew :app:testDebugUnitTest    # prayer-time calculator, helpers
```

## Configuration

- Package `com.x2softinc.masthan`, minSdk 26, targetSdk 35, version 2.0 (matches iOS `MARKETING_VERSION`).
- App code for the version check: `MGMKAND1` (iOS uses `MGMKIOS1`), see `SecureConfiguration` in `data/VersionService.kt`.
- API base URL / device code defaults are in `data/Prefs.kt` (same values as iOS; the default URL is HTTPS).
- `res/xml/network_security_config.xml` still allows cleartext for `x2softinc.com` in case the server's `overrideURL` points at an HTTP host; remove it if that never happens.
