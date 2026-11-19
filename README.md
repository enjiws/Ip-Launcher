# Enjiws Launcher V0.2 — Liquid Glass

An Android HOME launcher prototype with a refined iOS-inspired Liquid Glass look.

## V0.2 changes
- Refined translucent wallpaper, glass widgets, rounded icon tiles and dock.
- Live clock/date and battery percentage widgets.
- App grid search and a separate app-library page.
- Swipe left/right to switch pages; swipe down to open the launcher Control Center.
- Control Center shortcuts open Android's native Internet, Bluetooth, Airplane, Display and Sound settings where supported.
- Expandable in-launcher Dynamic Island visual.
- Reduced background work: clock refreshes every 30 seconds and app list is cached for the launcher session.

## Android limitations
This remains a third-party launcher, not a replacement for HyperOS System UI.
- Lock Screen is an in-launcher preview; Android's secure lock screen remains in charge.
- Notification Center and system status bar are still provided by Android/HyperOS.
- Dynamic Island is visible inside the launcher only; it is not a system-wide overlay.
- Control Center shortcuts open native settings; they do not silently toggle protected system radios.
- Weather is intentionally not shown as live data until a real weather provider is configured.

## Build
Requires JDK 17 and Android SDK platform 35.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
