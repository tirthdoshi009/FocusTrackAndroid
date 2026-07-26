# FocusTrack, Android App Usage and Focus Tracker

An Android port of the DeepTrack browser extension. FocusTrack measures how long
you spend in each app (Instagram, Twitter/X, browsers, work apps and more),
categorizes them as **risky**, **productive**, or **neutral**, and turns that
into a daily **focus score**. All data stays on the device.

## How it works (Tier 1, app level)

FocusTrack reads Android's `UsageStatsManager` foreground and background events to
compute per app foreground time for today, maps each package to a category
(`data/AppCategories.kt`), and shows a Today summary with a focus score and a per
app breakdown.

Browsers are tracked as whole apps. The app does **not** read URLs or on screen
content, so it needs only the standard "Usage access" permission and is safe for
the Play Store.

## Features

* **Today**: focus score ring, screen time, a category proportion bar, and a per
  app breakdown you can recategorize with a tap.
* **Trends**: the last 7 days with a category donut and a daily stacked
  breakdown.
* **Discover**: curated productivity apps grouped by category, hiding the ones
  you already have installed.
* **Home screen widget**: focus score, screen time, and category split, updated
  instantly when your data changes.
* About 150 popular apps categorized by default, so the score works right away.
  Any app can be recategorized, and your choices always win over the defaults.

## Permissions

`PACKAGE_USAGE_STATS`, a special access that the user enables under "Usage
access" in Settings. The app deep links there from the onboarding screen.

## Requirements

Android Studio (bundled JDK and SDK). Min SDK 26, target and compile SDK 36.

## Build and run

Open the project in Android Studio and let it sync (it will install the matching
SDK and build tools), then Run on an emulator or device. Or from the command line
once the SDK is configured:

```powershell
.\gradlew.bat assembleDebug
```

## Stack

* Kotlin and Jetpack Compose (Material 3)
* Room for on device persistence
* WorkManager for background jobs
* Jetpack Glance for the home screen widget
