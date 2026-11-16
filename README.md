# Magnifier & Digital Microscope

A Native Android **Magnifying Glass & Digital Microscope** application built with **Kotlin**, **Jetpack Compose**, and **CameraX + Camera2Interop**. Designed to turn your smartphone camera into an instant jeweler's loupe for inspecting circuit boards, reading microscopic serial numbers, checking splinters, or examining plant leaves.

## Features

- **Multi-Lens Auto-Discovery**: Automatically detects rear physical and logical lenses (`Main`, `Macro`, `0.6x Wide`, `Tele`) via Camera2 characteristics.
- **Precision Zoom**: Pinch-to-zoom, precision slider, and quick magnification presets (`1×`, `2×`, `5×`, `10×`).
- **Tap-to-Focus & AF Lock**: Animated tap-to-focus reticle with an `AF LOCK` toggle to prevent focus hunting on close-up subjects.
- **Manual Focus Distance**: Fine diopter slider (`CONTROL_AF_MODE_OFF` + `LENS_FOCUS_DISTANCE`) with approximate centimeter distance readout (`~cm` to `∞`).
- **Illumination & Exposure**: Flashlight/torch toggle with Android 15+ variable torch strength slider (with graceful fallback on older devices) and Exposure Compensation (`EV`) slider.
- **Freeze Frame with Pan & Zoom**: Pause the live feed to inspect high-resolution details without hand shake, supporting up to `8×` pinch-to-zoom and drag-to-pan.
- **Real-Time Inspection Filters**: `Normal`, `Contrast`, `Invert` (ideal for faint laser-etched serial numbers), and `Mono` (grayscale).
- **Focus Peaking Overlay**: Real-time Sobel edge-detection highlight on sharp in-focus areas to help dial in manual focus.
- **Measurement Reticle & Grid**: Toggleable crosshair and reference grid overlay.
- **Capture & Quick Share**: Save full-resolution snapshots (with active filter baked in) to `Pictures/Magnifier` via `MediaStore`, or share directly via `FileProvider`.

## Architecture

```text
app/src/main/java/com/magnifier/app/
├── MainActivity.kt                  # Edge-to-edge host, keep-screen-on, runtime permissions
├── camera/
│   ├── CameraLensInfo.kt            # Lens model & optical/torch metadata
│   ├── CameraLensDiscovery.kt       # Camera2 rear physical/logical lens discovery
│   └── CameraController.kt          # CameraX + Camera2Interop focus, zoom, torch, EV & capture
├── processing/
│   ├── InspectionFilter.kt          # 4x5 ColorMatrix filters (live + bitmap baking)
│   └── FocusPeakingProcessor.kt     # Real-time Sobel luma edge-detection analyzer
├── storage/
│   └── SnapshotRepository.kt        # MediaStore gallery save & FileProvider sharing
└── ui/
    ├── MagnifierViewModel.kt        # Unidirectional UI state management
    ├── MagnifierScreen.kt           # Glassmorphic loupe UI, gestures & overlays
    └── theme/Theme.kt               # Dark-first optical instrument theme
```

## Requirements & Build

- **Android Studio / Gradle**: AGP 8.9.1, Gradle 8.13, JDK 17
- **SDK**: `minSdk 26` (Android 8.0) / `targetSdk 35` (Android 15)

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build release (production) APK
./gradlew assembleRelease
```
