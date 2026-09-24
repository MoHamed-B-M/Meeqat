# Meeqat — Azan App — Full Build Plan

> **Tagline:** A calm, precise, and beautiful Azan app — crafted for accuracy, serenity, and personalization.
> **Design System:** 100% Material 3 Expressive
> **Package:** `com.meeqat.azan` | **Codename:** Meeqat | **Min SDK:** 26 | **Target SDK:** 36

---

## 1. Vision & Principles

**Vision:** Meeqat delivers the most accurate prayer times for where you are — not the nearest city — with full user control over tuning and sound.

**Principles:**
1.  **Accuracy over approximation:** GPS-based calculation with high-precision algorithms, not coarse city APIs.
2.  **Respectful by default:** Exact alarms, DND-aware, no missed Azan.
3.  **Expressive but serene:** M3 Expressive motion/shape/color used to feel spiritual and calm, not playful.
4.  **Offline-first:** Times, Qibla and sounds work without internet. Last location cached.
5.  **User is the authority:** Manual adjustment and local sounds are first-class features, not hidden toggles.

---

## 2. Tech Stack

| Layer | Choice |
| :--- | :--- |
| **Language/Platform** | Kotlin 2.0+, Jetpack Compose, Material 3 `1.12+` (Expressive) |
| **Architecture** | MVVM + Clean Architecture, Single Activity, Navigation Compose |
| **DI** | Hilt (or Koin) |
| **Location** | `play-services-location` (`FusedLocationProviderClient`), `Geocoder` / `Geocoder` + `MapLibre` or `Google Maps Compose` |
| **Calculation** | Local library: `com.batoulapps:adhan` (Java) — no network dependency. Supports all major methods. |
| **Date/Time** | `kotlinx-datetime`, `HijriDate` (Umm Al-Qura via `com.github.msarhan:ummalqura-calendar`) |
| **Storage** | Room (prayer history, offsets), DataStore Preferences (settings), DocumentFile/SAF for sounds |
| **Background** | `WorkManager` + `AlarmManager.setExactAndAllowWhileIdle()` + `SCHEDULE_EXACT_ALARM`, `ForegroundService` for playback, `BroadcastReceiver` for `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `TIME_SET` |
| **Audio** | `ExoPlayer` / `MediaPlayer` with `AudioAttributes.USAGE_ALARM`, Audio Focus, DND bypass handling |
| **Sensors** | `SensorManager` (`TYPE_ROTATION_VECTOR`) for Qibla |

---

## 3. Prayer Time Engine

**A. Inputs:**
- `latitude`, `longitude`, `altitude` (optional), `timezone`, `Gregorian Date`
- `CalculationMethod`: UmmAlQura (default), Egyptian, Karachi, MWL, Dubai, MoonsightingCommittee, Kuwait, Qatar, Singapore, + Custom Angles
- `Madhab`: Shafi'i / Hanafi (for Asr)
- `HighLatitudeRule`: AngleBased, MiddleOfNight, OneSeventh

**B. Flow:**
```
LocationRepository (GPS/Cached/Manual)
    ↓
CalculationRepository.getTimes(lat, lng, method, madhab, highLatRule)
    ↓ (Adhan library - local)
RawPrayerTimes
    ↓
ManualAdjustRepository (+/- 60 min per prayer)
    ↓
DailyPrayerTimes (Room Entity + Flow)
```

**C. Caching:** Calculate 30 days ahead on every location/method change. Store in Room `daily_prayer` table. A `MidnightWorker` refreshes at 00:05 daily. If GPS fails, use last known location; if none, prompt manual city picker.

**D. Hijri:** Convert Gregorian to Hijri via Umm Al-Qura for display on Home and Calendar.

---

## 4. GPS & Location UX

**Permission Flow:** M3 Expressive bottom sheet with illustration explaining *why* (accuracy + Qibla). Request `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`. `ACCESS_BACKGROUND_LOCATION` only if user enables auto-update on travel.

**States:**
- `Precise (GPS)` - green dot, city/district via reverse geocoder.
- `Manual (Pinned)` - orange, shows "Using manual location".
- `Cached (Offline)` - grey, "Last updated 2h ago".

**Logic:**
- Launch: `getCurrentLocation(PRIORITY_HIGH_ACCURACY)` with 8s timeout. Fallback to `lastLocation`.
- If `distance(lastLocation, newLocation) > 30km`, show M3 dialog: "You've moved to [New City]. Update prayer times?"
- Manual override: Search bar (Geocoder) + draggable map pin. Saved as `ManualLocation` which suspends GPS auto-update until user taps "Use my location again".
- Qibla bearing re-calculated on every location change.

---

## 5. Manual Adjustment

**Model:**
```kotlin
data class ManualOffset(
  val fajr: Int = 0, val sunrise: Int = 0, val dhuhr: Int = 0,
  val asr: Int = 0, val maghrib: Int = 0, val isha: Int = 0
) // -60..+60
```

**UI:** `Settings > Prayer Adjustments`
- List with `M3 Slider` (-30..+30, haptics on tick) + `+/-` `IconButton` for precision.
- Live preview row: `Calculated: 05:12 → Adjusted: 05:14` in `M3 AssistChip`.
- Global safety offset toggle (+2 min).
- **Important:** Adjustment is applied only at the UI/Display layer; the original `RawPrayerTimes` is kept in DB for debugging and re-calculation. Reset button per prayer + Reset All.

**Storage:** `DataStore` or Room `manual_offset` table single row.

---

## 6. Local File Sound Customization

**A. Picking:**
- `ActivityResultContracts.OpenDocument` with `audio/*` (`mp3, m4a, wav, ogg, flac`).
- `takePersistableUriPermission()` to survive reboots. Store `Uri.toString() + displayName + duration`.
- Also provide 6-8 built-in Azans in `res/raw` (Makkah, Madinah, Al-Aqsa, Egypt, etc.) with beautiful M3 cards and preview waveform.

**B. Per-Prayer Sound:**
```kotlin
data class SoundConfig(
  val useSameForAll: Boolean = true,
  val fajrUri: String? = null,
  val defaultUri: String? = null, // used if useSameForAll
  val dhuhrUri: String? = null, val asrUri: String? = null,
  val maghribUri: String? = null, val ishaUri: String? = null
)
```
- UI: `SoundPickerSheet` with tabs: `Built-in | My Files`. Each local file shows name, duration, delete. Preview button plays via `ExoPlayer` with seek.

**C. Reliability:**
- Before scheduling, copy SAF Uri to app `cache/azan_cache/` for `AlarmManager` to have a File descriptor. Fallback to default if copy fails.
- Validate on picking: `< 5 min`, supported mime, show `Snackbar` error with M3 styling if invalid.
- Detect revoked permission on app start (`persistedUriPermissions`): show banner "Your custom Azan file is no longer accessible, tap to re-select" and fallback to default.

**D. Permissions:** `READ_MEDIA_AUDIO` (Android 13+), gracefully handle denial with built-ins only.

---

## 7. App Architecture & Navigation

```
app/src/main/java/com.meeqat.azan/
├── MeeqatApp.kt (Hilt App)
├── MainActivity.kt (Single Activity, edge-to-edge)
├── data/
│   ├── local/ (Room: AppDatabase, DailyPrayerDao, OffsetDao, SoundDao)
│   ├── repo/ (LocationRepository, CalculationRepository, SoundRepository, SettingsRepository)
│   └── prefs/ (DataStore)
├── domain/
│   ├── model/ (DailyPrayerTimes, ManualOffset, SoundConfig, QiblaInfo)
│   └── usecase/ (GetTodayTimesUseCase, ScheduleAzanUseCase)
├── ui/
│   ├── home/ (HomeScreen, HomeViewModel, Countdown, PrayerCards)
│   ├── qibla/ (QiblaScreen, Compass, Level)
│   ├── calendar/ (CalendarScreen)
│   ├── settings/ (SettingsNavGraph, Location, Method, Adjust, Sound, Notifications, Appearance)
│   ├── components/ (MeeqatTopAppBar, PrayerCard, TipCard)
│   └── theme/ (M3 Expressive Color/Type/Shape/Motion)
├── receiver/ (BootReceiver, TimeZoneReceiver)
├── worker/ (PrayerScheduleWorker, MidnightRefreshWorker)
└── service/ (AzanForegroundService, AzanActivity - FullScreenIntent)
```

**Navigation:**
- `Scaffold` with `M3 Expressive NavigationBar` (Home | Qibla | Calendar | Settings)
- `AnimatedContent` with `MotionScheme.expressive()` for transitions.
- `Home` is start destination.

---

## 8. UI/UX — Material 3 Expressive Spec

**Inspiration:** Spiritual, warm, non-gaming. Think paper, light, and sand — not neon.

**Color:**
- **Seed:** `Deep Emerald #0D2C2A` + `Warm Sand #EADDC8` + `Gold #D9AD6A` as accent.
- **Dynamic Color:** Enabled on Android 12+ via `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme`. Fallback to custom seed via `ColorPaletteGenerator` (tonal spot).
- Tonal surfaces replace shadows. `surfaceContainerHigh` for main cards.

**Typography:**
- `DisplayLarge` (48sp, W400, -0.25 tracking) for countdown `02:14:05`.
- `HeadlineMedium` for prayer names, `TitleMedium` for cards.
- Emphasized weights (W600) for next prayer.

**Shape:**
- `Small: 8dp` (Chips), `Medium: 12dp` (TextFields), `Large: 16dp` (Buttons), `ExtraLarge: 28dp` (Main prayer cards), `Full: 9999` (FAB, Switch).
- Morph on selection: `28dp -> 16dp` with spring.

**Motion:**
- `MotionScheme.expressive()` springs: Card press `scale 0.97`, FAB morph, `Emphasized 500ms` for page enter, `Standard 300ms` for lists.
- Countdown tick: `fadeIn + scaleIn` with `spring`.

**Elevation:** Tonal only. No shadows except for Azan full-screen overlay.

**Layout:**
- Phone: Single column, max width `840dp` centered, `WindowSizeClass` adaptive.
- Tablet/Foldable: `ListDetailPaneScaffold` (Home list + detail), `SupportingPane` for Qibla. Avoid hinge.
- Edge-to-edge with `WindowInsets`.

---

## 9. Screens in Detail

### Home (Today)
- **Top:** Hijri + Gregorian date, Location pill (city + `Precise` dot, tappable to map).
- **Hero:** Next prayer card — `M3 Card (extraLarge)` with countdown, prayer name, time, and `M3 LinearWavyProgressIndicator` for time left.
- **Grid:** 6 prayers in 2-column `LazyVerticalGrid` (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha). Current prayer highlighted with `primaryContainer`. Passed prayers at `50%` alpha. Each card shows adjusted time, original time (small, strikethrough if adjusted), and sound icon if custom.
- **Mini:** Qibla card (bearing + distance) + "Time to Isha" footer.

### Qibla
- Fullscreen `Canvas` compass with `24` tick marks, Kaaba icon, needle with `animateFloatAsState` spring on sensor change.
- Sensor fusion via `SensorManager.getRotationMatrixFromVector()`, filtered with `LowPassFilter`.
- Level indicator, "Hold flat" prompt, calibration animation (figure-8) if accuracy low.
- Bottom sheet with distance to Kaaba and coordinates.

### Settings
- Grouped via `SegmentedPreferenceGroup` (as in Flambo but serene).
- Sections: Location, Calculation Method, Adjustments, Sound, Notifications, Appearance, About.

---

## 10. Notifications & Playback

- **Pre-Azan (15 min before):** Silent notification: "Asr in 15 minutes • 15:42".
- **At Azan:** `AlarmManager` exact -> `AzanForewgroundService` -> `FullScreenIntent` (`AzanActivity` over lockscreen) + `MediaPlayer` with `USAGE_ALARM` (bypasses DND if user allowed). UI: Prayer name, time, `Dismiss` / `Snooze 5m` (reschedules via `WorkManager`).
- **Settings:** `Azan Mode: Full Azan / Notification only / Silent (vibrate)` per prayer.

---

## 11. Data & Permissions

**Room Entities:**
```kotlin
@Entity
data class DailyPrayerEntity(
  @PrimaryKey val date: String, // yyyy-MM-dd
  val fajr: Long, val sunrise: Long, val dhuhr: Long, val asr: Long, val maghrib: Long, val isha: Long,
  val method: String, val lat: Double, val lng: Double
)
```

**Permissions:**
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` (optional)
- `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED`, `READ_MEDIA_AUDIO`, `VIBRATE`

---

## 12. Roadmap

**Phase 1 — Foundation (Week 1-2):** Project scaffold, M3 Expressive theme, Location + Adhan engine, Home Today + 30-day cache.

**Phase 2 — Core Ritual (Week 3-4):** Exact alarms + `ForegroundService` + Full-screen Azan, per-prayer sound picker (SAF), Manual Adjust sliders.

**Phase 3 — Polish (Week 5-6):** Qibla with sensor fusion, Calendar + Hijri, Widgets (Home + Lockscreen), Wear OS tile, Settings IA final, Play Store assets.

---

## 13. Risks & Mitigations

| Risk | Mitigation |
| :--- | :--- |
| `SCHEDULE_EXACT_ALARM` denied on Android 14+ | Gracefully degrade to `setWindow` + persistent "Missed Azan" check on app open. Show M3 education sheet. |
| SAF permission revoked after reboot | Detect `persistedUriPermissions` on launch; banner + fallback to built-in sound. |
| Sensor inaccuracy (Qibla) | Calibration animation + disclaimer + use `TYPE_ROTATION_VECTOR` (fused) not raw compass. |
| Battery optimization kills alarms | Request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with rationale, use `WorkManager` as watchdog to reschedule. |

---

## 14. What to Build First

1.  `libs.versions.toml` + `Hilt` + `Room` + `DataStore` setup.
2.  `MeeqatTheme` with dynamic color seed `0D2C2A`.
3.  `LocationRepository` + `CalculationRepository` with Adhan.
4.  `HomeScreen` countdown + 6 cards.
5.  `AzanWorker` + exact alarm proof-of-concept.

*End of Plan — Ready to scaffold `com.meeqat.azan`.*
