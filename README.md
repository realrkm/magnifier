# Magnifier & Digital Microscope

A Native Android **Magnifying Glass & Digital Microscope** application built with **Kotlin**, **Jetpack Compose**, and **CameraX + Camera2Interop**. Designed to turn your smartphone camera into an instant jeweler's loupe for inspecting circuit boards, reading microscopic serial numbers, checking splinters, or examining plant leaves.

## Features

- **Multi-Lens Auto-Discovery**: Automatically detects rear physical and logical lenses (`Main`, `Macro`, `0.6x Wide`, `Tele`) via Camera2 characteristics.
- **Precision 200× Hybrid Zoom**: Hardware optical/sensor zoom combined with smooth hybrid viewport magnification up to `200×` using a logarithmic precision slider and quick presets (`1×`, `5×`, `20×`, `50×`, `100×`, `200×`).
- **On-Device Computational Clarity (`5×+` Ultra Clarity)**: Automatically engages Camera2 `EDGE_MODE_HIGH_QUALITY` & `NOISE_REDUCTION_MODE_HIGH_QUALITY`, live micro-contrast enhancement, **CameraX Extensions (`ExtensionMode.AUTO`)**, and a **3-frame temporal-averaging super-resolution burst** (`SuperResolutionEnhancer.mergeMultiFrameAndEnhance`) when freezing or capturing at `5×`–`200×`.
- **Google ML Kit On-Device Suite**:
  - **Multi-Script OCR (`TextScanner`)**: Reads microscopic serial numbers and chip markings across **Latin, Chinese, Japanese, Korean, and Devanagari** scripts with one-tap clipboard copy.
  - **Entity Extraction (`EntityExtractorHelper`)**: Automatically parses URLs, emails, phone numbers, tracking numbers, and dates from OCR results into copyable chips.
  - **Micro-Barcode & DataMatrix Scanner (`BarcodeScanner`)**: Decodes tiny DataMatrix, QR, Aztec, Code128, and EAN/UPC codes under magnification.
  - **Object Detection & Auto-Focus (`ObjectDetectorHelper`)**: Draws bounding boxes around detected specimens/components and auto-focuses on the primary target.
  - **Smart Scene Presets (`SmartPresetLabeler`)**: Uses ML Kit Image Labeling to suggest optimal inspection filters and torch settings for circuits, plants, insects, or textiles.
  - **Specimen Isolation (`SubjectIsolator`)**: Uses ML Kit Subject Segmentation to dim and desaturate the background while keeping the foreground specimen in sharp focus.
- **Tap-to-Focus & AF Lock**: Animated tap-to-focus reticle with an `AF LOCK` toggle to prevent focus hunting on close-up subjects.
- **Manual Focus Distance**: Fine diopter slider (`CONTROL_AF_MODE_OFF` + `LENS_FOCUS_DISTANCE`) with approximate centimeter distance readout (`~cm` to `∞`).
- **Illumination & Exposure**: Flashlight/torch toggle with Android 15+ variable torch strength slider (with graceful fallback on older devices) and Exposure Compensation (`EV`) slider.
- **Freeze Frame with Pan & Zoom**: Pause the live feed to inspect high-resolution details without hand shake, supporting up to `8×` pinch-to-zoom and drag-to-pan.
- **Real-Time Inspection Filters**: `Normal`, `Contrast`, `Invert` (ideal for faint laser-etched serial numbers), and `Mono` (grayscale).
- **Focus Peaking Overlay**: Real-time Sobel edge-detection highlight on sharp in-focus areas to help dial in manual focus.
- **Measurement Reticle & Grid**: Toggleable crosshair and reference grid overlay.
- **Capture & Quick Share**: Save full-resolution snapshots (with active filter baked in) to `Pictures/Magnifier` via `MediaStore`, or share directly via `FileProvider`.

## Ideal Use Cases & Recommended Tools

| Use Case | What to Inspect | Recommended Tools |
| :--- | :--- | :--- |
| **Electronics & PCBs** | Solder bridges, lifted SMD pads, hairline trace cracks, laser-etched IC markings | `Macro` lens + `Torch` + `Invert` filter + `OCR` + `Barcode` (DataMatrix/QR) |
| **Vehicle Paint & Dents** | Shallow hail dings, door dents (15°–30° grazing angle), stone chips, clearcoat scratches | `2×–5×` zoom + `Contrast` + `Grid` + `Peaking` + lower `EV` |
| **Serial Numbers & Fine Print** | Watch/jewelry serials, SIM trays, cables, medicine labels, imported hardware | `5×–50×` zoom + `AI Clarity` + `AF LOCK` + Multi-Script `OCR` + `Entity Extraction` |
| **First Aid & Skin** | Tiny splinters, glass shards, thorns, ticks | `Torch` + `Freeze Frame` (`8×` pan/zoom) + `Manual Focus` + `Peaking` |
| **Gardening & Pests** | Leaf undersides, spider mites, aphids, fungal spores, leaf-vein damage | `Smart Preset` + `Contrast` + `Objects` (auto-focus) + `Isolate` |
| **Coins, Gems & Collectibles** | Mint marks, hallmarks, gemstone inclusions, watch movements, micro-printing | `EV` slider + `Grid` reticle + `Isolate` + `5×+` super-res `Capture` |
| **Textiles & Print Quality** | Fabric weave density, stitching, 3D-print layer lines, halftone/pixel grids | `Macro` lens + `Contrast` / `Mono` + `Freeze Frame` pan/zoom |

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
