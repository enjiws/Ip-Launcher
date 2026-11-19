# Enjiws Launcher

An Android HOME launcher prototype with an iOS 26-inspired glass aesthetic.

## Included
- Android HOME/default launcher intent
- Installed-app grid and app launching
- Search field
- Glass-style widgets and dock
- Expandable Dynamic Island-style visual
- In-app Control Center mockup
- Lock-screen-inspired preview
- GitHub Actions debug APK build

## Build
Requires JDK 17 and Android SDK platform 35.

```bash
gradle assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## Install / activate
Install the APK, press Home, then select **Enjiws Launcher** as the default Home app.
You can also change it in Android Settings > Apps > Default apps > Home app.

## Important Android limitations
This is a third-party launcher, not a system UI replacement. The Control Center currently provides a visual preview and local demo toggles; it cannot directly toggle protected system radios or change system brightness without using Android system panels or privileged access. The lock screen is an in-app preview and does not replace Android's secure lock screen. Dynamic Island is an in-launcher visual component, not an overlay over other apps. Weather widget currently uses placeholder display data.
