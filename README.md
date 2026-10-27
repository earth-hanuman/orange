# Orange

A system-level blue-light / color-filter app for Android — a "real" warmth and
grayscale filter that drives the display directly, aimed at going warmer and
deeper than the stock Night Light. Built for a Pixel running
[GrapheneOS](https://grapheneos.org/), no root required.

Package: `earth.hanuman.orange`

## Features

- **Warmth** — a deep amber/red tint with adjustable hue (red ↔ amber) and
  intensity, drawn as a full-screen overlay so it can go warmer and more
  saturated than stock Night Light.
- **Grayscale** — true desaturation via the system daltonizer (on/off).
- **Dim** — the real Extra Dim transform (`reduce_bright_colors`), adjustable.
- **Schedule** — automatic on/off between two times, re-armed after reboot.
- **Quick Settings tile** — toggle the whole filter from the notification shade.
- **Bedtime preset** — one tap for strong warmth + dim.

## Requirements

- Android 8.0+ (`minSdk 26`); developed against `compileSdk`/`targetSdk 36`.
- JBR/JDK 21, Android SDK API 36.1.

## Build

Use the Gradle **wrapper** (pinned to Gradle 8.13 for AGP 8.13.0 — a newer
system Gradle will not work):

```bash
./gradlew :app:assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

## Permissions setup

Two permissions must be granted once after install:

1. **`WRITE_SECURE_SETTINGS`** — for grayscale and dim (the real display
   transforms). It cannot be granted from the UI; with USB or wireless
   debugging enabled, run once:

   ```bash
   adb shell pm grant earth.hanuman.orange android.permission.WRITE_SECURE_SETTINGS
   ```

   The grant is per-user: repeat it under each GrapheneOS profile that uses the
   app.

2. **Display over other apps** (`SYSTEM_ALERT_WINDOW`) — for the warmth overlay.
   Grant it from the in-app prompt when asked.

## Privacy

- **No `INTERNET` permission** — Orange never touches the network.
- No analytics, no third-party SDKs. Only Kotlin + Jetpack Compose and the
  AndroidX libraries needed to run it. Settings are stored in plain
  `SharedPreferences`.

## Project layout

- `MainActivity.kt` — Compose UI (the dial, sliders, chips, schedule dialog).
- `FilterController.kt` — grayscale + dim via `Settings.Secure`.
- `OverlayService.kt` — the warmth overlay (foreground service).
- `Orchestrator.kt` — turns persisted state into actual effects.
- `ScheduleManager.kt` / `ScheduleReceiver.kt` — the time schedule + boot re-arm.
- `FilterTileService.kt` — the Quick Settings tile.
- `Prefs.kt` — persisted settings.

## Hosting & distribution

Source is hosted on Nostr git ([NIP-34](https://github.com/nostr-protocol/nips/blob/master/34.md))
via [ngit](https://gitworkshop.dev/). Releases are intended for
[Zapstore](https://zapstore.dev/).
