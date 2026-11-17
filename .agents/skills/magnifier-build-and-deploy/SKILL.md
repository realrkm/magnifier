---
name: magnifier-build-and-deploy
description: >-
  Build, unit test, R8 release signing, ADB device installation, and crash
  debugging workflow for the Magnifier Android project. Use before committing
  changes or deploying a production APK to a connected phone.
---

# Magnifier Build, Test & Deploy Runbook

## Environment Requirements
- **JDK**: Always run Gradle with **JDK 17** (`$env:JAVA_HOME="C:\Program Files\Java\jdk-17"`). Newer system JDKs (e.g. JDK 27) are incompatible with Gradle 8.13 / AGP 8.9.1.
- **Android SDK**: Configured via `local.properties` (`C:\Users\ADMIN\AppData\Local\Android\Sdk`, `compileSdk = 35`, `minSdk = 26`).

## Standard Verification & Production Build
Run unit tests and build the R8-minified, resource-shrunk, signed release APK:
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
.\gradlew.bat testDebugUnitTest assembleRelease --no-daemon
```
Output APK: `app\build\outputs\apk\release\app-release.apk`.

## Keystore & Git Hygiene
- Never commit `release.keystore`, `*.jks`, or `local.properties` (all are in `.gitignore`).
- If `release.keystore` is missing locally, `app/build.gradle.kts` falls back to debug signing for `release` builds.

## Deploying to Connected Phone & Checking Crash Logs
1. List attached devices:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
   ```
2. Install the production APK:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\release\app-release.apk"
   ```
3. Inspect crash logs if an issue occurs on-device:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -b crash -d
   ```
