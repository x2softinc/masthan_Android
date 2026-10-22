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
# Optional: API key for the app-version check (iOS keeps it in Info.plist as VERSION_API_KEY)
VERSION_API_KEY=...
```

```bash
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug
./gradlew :app:testDebugUnitTest    # prayer-time calculator, helpers
```

## Configuration

- Package `com.x2softinc.masthan`, minSdk 26, targetSdk 35, version 2.0 (matches iOS `MARKETING_VERSION`).
- API base URL / device code defaults are in `data/Prefs.kt` (same values as iOS).
- The API is plain HTTP on `x2softinc.com`; `res/xml/network_security_config.xml` allows cleartext for that domain only.
