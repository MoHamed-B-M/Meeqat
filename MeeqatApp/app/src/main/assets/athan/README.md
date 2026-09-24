# Meeqat — Athan Audio Assets

Place your local Athan `.mp3` files here. The app will copy them to `cache/azan_cache/` for reliable `AlarmManager` playback and also supports SAF-picked files from device storage.

## Recommended placement

- **Bundled (built-in)**: `app/src/main/res/raw/` (preferred for 6–8 built-in Athans)
  - Example: `res/raw/athan_makkah.mp3`, `res/raw/athan_madinah.mp3`, `res/raw/athan_aqsa.mp3`
  - Keeps assets inside APK, always available offline, survives SAF revocation.
  - Reference in code via `R.raw.athan_makkah` or `android.resource://com.meeqat.azan/raw/athan_makkah`.

- **Assets folder (this directory)**: `app/src/main/assets/athan/`
  - Example: `assets/athan/makkah.mp3`, `assets/athan/egypt.mp3`
  - Accessed via `assets.open("athan/makkah.mp3")` and copied to cache on first use.
  - Good for larger collections or dynamic listing via `AssetManager.list("athan")`.

- **User-picked (SAF)**: `ActivityResultContracts.OpenDocument` with `audio/*` (`mp3,m4a,wav,ogg,flac`)
  - Stored as persistable `Uri` in `SoundConfig` + copied to `cache/azan_cache/` before scheduling.
  - Detect revocation via `contentResolver.persistedUriPermissions` on app start; fallback to default and show banner.

## Requirements

- Duration `< 5 min`, supported MIME `audio/mpeg`, `audio/mp4`, `audio/x-wav`, `audio/ogg`
- Use `AudioAttributes.USAGE_ALARM` + `AudioFocus` so playback bypasses DND when user allowed.
- For exact alarms: copy SAF Uri to `cache/azan_cache/<prayer>.mp3` for `MediaPlayer`/`ExoPlayer` FileDescriptor stability across reboots.

## How the scheduler uses them

1. `SoundRepository` stores `SoundConfig` (per-prayer URIs) in DataStore.
2. `AthanScheduler.scheduleToday()` validates each URI, copies to cache, falls back to `res/raw` default if missing.
3. `AthanReceiver` → `AzanForegroundService` plays via `ExoPlayer` with `USAGE_ALARM` and shows high-priority notification with Stop/Snooze.

## Adding a new built-in Athan

1. Drop `my_athan.mp3` into `app/src/main/res/raw/` (lowercase, underscores, no spaces).
2. Add entry in `SoundPickerSection` Built-in tab list.
3. Rebuild — CI will bundle it in `Meeqat-*.apk`.

## User flow

- Settings → Sound → Tabs: **Built-in | My Files** → Pick → Preview via ExoPlayer seek → Save.
- Per-prayer toggle: "Use same for all" vs per-prayer mapping (Fajr/Dhuhr/Asr/Maghrib/Isha).
