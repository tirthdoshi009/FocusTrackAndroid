# FocusTrack — Android App Usage & Focus Tracker

An Android port of the DeepTrack browser extension. FocusTrack measures how long
you spend in each app (Instagram, Twitter/X, browsers, work apps…), categorizes
them as **risky / productive / neutral**, and turns that into a daily **focus
score**. All data stays on the device.

## How it works (Tier 1 — app-level)

FocusTrack reads Android's `UsageStatsManager` foreground/background events to
compute per-app foreground time for today, maps each package to a category
(`data/AppCategories.kt`), and shows a Today summary with a focus score and a
per-app breakdown.

Browsers are tracked as whole apps — the app does **not** read URLs or on-screen
content, so it needs only the standard "Usage access" permission and is
Play-Store-safe.

## Permissions

- `PACKAGE_USAGE_STATS` — special access; the user enables "Usage access" in
  Settings. The app deep-links there from the onboarding screen.

## Requirements

- Android Studio (bundled JDK + SDK). Min SDK 26, target/compile SDK 36.

## Build & run

Open the project in Android Studio and let it sync (it will install the matching
SDK/build-tools), then Run on an emulator or device. Or from the CLI once the
SDK is configured:

```powershell
.\gradlew.bat assembleDebug
```

## Roadmap

See milestones in the session plan: Room persistence, WorkManager periodic
aggregation + 90-day purge, per-category history, settings to recategorize apps,
and a daily summary notification.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Room (persistence) · WorkManager (background jobs)
