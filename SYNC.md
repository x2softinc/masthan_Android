# Keeping Android in sync with the iOS app

The iOS app lives in [`x2softinc/Masthan`](https://github.com/x2softinc/Masthan) (Swift/SwiftUI).
This repo is its Android port (Kotlin/Jetpack Compose). **The Masthan repo is the source of
truth and is never modified from here** - changes flow one way, Masthan -> Android.

Last synced Masthan commit: `24f2bffc5db80344a5a30f8e9fe3e1c78816cae0`

## Workflow

1. `scripts/masthan_changes.sh` - lists Masthan commits and changed files since the commit above.
2. Port each change to the matching Kotlin file (map below).
3. Build and test: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
4. Update the hash above in the same commit as the port.

## File map

| Swift (`Masthan/`) | Kotlin (`app/src/main/java/com/x2softinc/masthan/`) |
|---|---|
| `MasthanApp.swift` | `MainActivity.kt`, `MasthanApplication.kt` |
| `AppMetadata.swift` | `AppMetadata.kt` |
| `Models/Constants.swift` | `Constants.kt` |
| `Models/ApiPaths.swift` | `data/ApiPaths.kt` |
| `Models/{AdhanTime,Announcement,AnnualTimes,FixedIqamaTime,IqamaTime,MasjidInfo,Payloads,PrayerSetting,TimeDelay,VersionInfo}.swift` | `data/Models.kt` |
| `Models/AdhanSoundOption.swift` | `data/AdhanSoundOption.kt` |
| `Extensions/ColorExtension.swift`, color assets | `ui/Theme.kt` |
| `helper/{DeviceCodeHelper,UrlHelper}.swift`, `@AppStorage` | `data/Prefs.kt`, `ui/PrefState.kt` |
| `helper/UserFriendlyError.swift` | `data/UserFriendlyError.kt`, `ui/Components.kt` |
| `helper/PrayerTimesCalculator.swift` | `data/PrayTimesCalculator.kt` |
| `Tools+Items/Dates+Functions.swift` | `util/DateFunctions.kt` |
| `Services/AuthService.swift` | `data/AuthService.kt`, `data/Http.kt` |
| request/retry logic repeated in each `Services/*Service.swift` | `data/ApiClient.kt` |
| `Services/{Masjid,PrayerSettings,TimeDelay,IqamaTimes,AdhanTimes,Version}Service.swift` | `data/*Service.kt` |
| `Services/AppVersionGate.swift` | `ui/AppVersionGate.kt` |
| `Managers/LocationManager.swift` | `data/LocationTracker.kt` |
| `Managers/NotificationScheduler.swift` | `notifications/*.kt` |
| `ViewModels/TimesViewModel.swift` | `viewmodel/TimesViewModel.kt` |
| `Views/ContentView.swift` | `ui/MainScreen.kt` |
| `Views/DashboardView.swift` | `ui/DashboardScreen.kt` |
| `Views/QiblaView.swift` | `ui/QiblaScreen.kt` |
| `Views/MasjidView.swift` | `ui/MasjidScreen.kt` |
| `Views/SettingsView.swift` | `ui/SettingsScreen.kt` |
| `Views/AdhanSoundSelectionView.swift` | `ui/AdhanSoundScreen.kt` |
| `Views/PrayerSettingsView.swift` | `ui/PrayerSettingsScreen.kt` |
| `Views/EULAView.swift` | `ui/EulaScreen.kt` |
| `Views/LegalDocumentView.swift` | `ui/LegalDocumentScreen.kt` |
| `*.mp3` adhan sounds | `res/raw/<lowercase name>.mp3` |
| `Assets.xcassets` compass images | `res/drawable[-night]/` |
| `AppIcon.appiconset` | `res/mipmap-*/ic_launcher_foreground.png` (cleaned artwork) + adaptive icon XML; monochrome / notification icons are the vector mosque glyph |

## Deliberately not ported

- `Services/AdhanIqamaService.swift`, `Services/NotificationService.swift` - legacy CoreData/JavaScriptCore
  code that no longer compiles against the current models (the iOS app does not use it).
- `Views/CalendarView.swift` - disabled in the iOS tab bar (commented out in `ContentView.swift`).
- Calendar and embedded map are not wanted on Android (decided by the owner).
- `*.caf` sounds, the `" 2"` duplicate asset files, `.xcodeproj`, `Info.plist` (ATS exception ->
  `res/xml/network_security_config.xml`), `PrivacyInfo.xcprivacy`.
- The dead `PrayerCardView` / `prayerRows` / `jummaSection` code in `DashboardView.swift`.

## Intentional platform differences

- **Maps**: iOS embeds an Apple Maps view. Android intentionally has no map; it shows the coordinates and
  opens the user's maps app for directions.
- **App code**: the version check uses `MGMKAND1` on Android (iOS: `MGMKIOS1` once updated; currently `ALBIOS1`).
- **Notifications**: iOS uses repeating calendar triggers. Android schedules one exact alarm per
  prayer and re-schedules the next day (with that day's recalculated time) after each fires.
- **Adhan sound**: Android notification-channel sounds are immutable, so each sound gets its own channel.
- **Terms of Use**: the Apple-specific wording of the EULA template is adapted to Google Play.
- **Settings screen "Open Settings"** opens the Android app-info page.
- `TimesViewModel.load` fetches prayer settings *before* computing rounded adhan / iqama times, so the
  settings apply on the first launch of the day (iOS applies them from the second load).
- Request handling (token refresh on 401, error mapping) is shared in `ApiClient`; the iOS
  `PrayerSettingsService` / `IqamaTimesService` retry path reuses a stale token - Android uses the fresh one.
