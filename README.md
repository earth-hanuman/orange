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

## Known limitations & design notes

Honest account of what works, what doesn't, and the trade-offs we hit building
this on a non-root, non-system app.

### Warmth is an overlay, not a true display transform

What we wanted: drive the *real* display warmth (`night_display`, the engine
behind the OS Night Light) so warmth is applied by the compositor beneath
everything — flicker-free and able to combine cleanly with other transforms.

What we found:

- `night_display_activated` / `night_display_color_temperature` **are** writable
  with `WRITE_SECURE_SETTINGS` and do take effect on GrapheneOS. **But** that is
  literally the stock Night Light: a single warmth (Kelvin) axis, amber only.
  It has **no hue control** (no red ↔ amber) and **cannot reach the deep,
  saturated red** we wanted — using it would just re-wrap the stock feature.
- To go beyond stock (pick a hue, go deeper/redder), warmth is instead drawn as
  a **full-screen colored overlay window**. This gives full hue + intensity
  control.

The cost of the overlay approach:

- **Brief flicker during system animations.** When rotating the screen or
  swiping to the home screen, the system plays a transition animation on a
  separate surface that a third-party overlay is **not** composited onto for a
  frame or two, so the tint briefly disappears. This is inherent to overlay
  windows and is **not fixable** without making Orange a privileged/system app
  (or root). Real transforms (grayscale, dim) do not have this.
- The overlay requires a **foreground service** with a persistent (low-priority)
  notification, since a live view needs a living process.
- It is a colored layer *on top* of the screen rather than a reduction of the
  blue the panel emits. It still absorbs blue and helps at night, but it is not
  the same mechanism as a hardware/compositor transform.

If a future version is made a privileged/system app, warmth should move to the
real `night_display` transform to remove the flicker.

### Grayscale is on/off only

Full desaturation works via the daltonizer, but there is **no partial / adjustable
grayscale**. Android does not expose partial desaturation to a non-system app
without applying a system color matrix (which needs root or system privileges).

### Everything is per-profile

Both the `WRITE_SECURE_SETTINGS` grant and the display settings are per Android
user. Under a second GrapheneOS profile the app is a separate install: the adb
grant must be repeated there, and its on/off state is independent.

### What does work well

- **Grayscale** and **Extra Dim** are real secure-settings transforms — applied
  by the system, instant, and flicker-free.
- Overlay **coverage** is correct: it fills the whole screen including under the
  status and navigation bars, and in every orientation (sized to a square of the
  largest display edge so rotation never leaves a gap).

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
via [ngit](https://gitworkshop.dev/). Releases go to
[Zapstore](https://zapstore.dev/) via the `zsp` CLI, configured by
`zapstore.yaml`.

Publishing a release (requires a signed APK + your Nostr key — see
`keystore.properties.template`):

```bash
# 1. build a signed release (after creating keystore.properties)
./gradlew :app:assembleRelease

# 2. check + publish (secrets stay with you)
export SIGN_WITH=nsec1...        # or a bunker:// remote signer
zsp check
zsp publish -q
```

## License

[GPL-3.0](LICENSE). Orange is free software; see the `LICENSE` file for the
full text.
