# PROWRAP ISO 24817 Android app

This directory is a standalone, native Android implementation of the PROWRAP
ISO 24817 calculator. It is intentionally separate from the root Streamlit
calculator and the macOS desktop launcher. After installation it calculates
fully offline: Python, Streamlit, localhost, and an internet connection are not
required.

The phone-first app targets Android API 26 and builds against/targets API 35.
Debug APK output is `app/build/outputs/apk/debug/app-debug.apk`; the release
APK is produced by `./gradlew :app:assembleRelease` after signing is configured.

Build from this directory with Java 21 and the Android SDK installed:

```bash
./gradlew testDebugUnitTest assembleDebug
```
