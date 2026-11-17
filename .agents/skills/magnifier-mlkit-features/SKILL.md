---
name: magnifier-mlkit-features
description: >-
  Pattern and checklist for adding or updating on-device Google ML Kit vision
  and NLP features (OCR, Barcode, Entity Extraction, Object Detection, Image
  Labeling, Subject Segmentation) in the Magnifier app.
---

# Magnifier Google ML Kit Integration

## Existing ML Kit Modules (`app/src/main/java/com/magnifier/app/processing/`)
- `TextScanner.kt`: Multi-script OCR (`OcrScript`: `LATIN`, `CHINESE`, `JAPANESE`, `KOREAN`, `DEVANAGARI`).
- `BarcodeScanner.kt`: All-format barcode & DataMatrix/QR scanner (`BarcodeScanning`).
- `EntityExtractorHelper.kt`: Extracts URLs, emails, phones, tracking numbers, dates from OCR text (`EntityExtraction`, downloads model if needed).
- `ObjectDetectorHelper.kt`: Single-image object detection returning normalized `RectF` boxes (`0f..1f`) for bounding-box overlay and auto-focus.
- `SmartPresetLabeler.kt`: Image labeling + luminance heuristic mapping scenes to `InspectionFilter` and torch suggestions.
- `SubjectIsolator.kt`: Subject segmentation foreground confidence mask dimming/desaturating background pixels.

## Checklist for Adding or Updating an ML Kit Feature
1. **Gradle Version Catalog (`gradle/libs.versions.toml`)**:
   - Avoid Kotlin keywords in alias segments (e.g., use `mlkit-objectdetection`, **not** `mlkit-object-detection`, because `libs.mlkit.object.detection` fails Kotlin DSL parsing).
2. **ProGuard / R8 (`app/proguard-rules.pro`)**:
   - Ensure `-keep class com.google.mlkit.** { *; }` and `-dontwarn` rules are preserved so `assembleRelease` (`isMinifyEnabled = true`) does not strip ML Kit reflection/JNI classes.
3. **Frame Acquisition in `MagnifierScreen.kt`**:
   - Reuse `withCurrentZoomedFrame(targetMaxDim = ...) { bmp, shouldRecycle -> ... }` so ML Kit runs on the already-zoomed & enhanced frame (or frozen frame) and recycles temporary bitmaps only after ML Kit's async callback completes.
4. **Unit Testing**:
   - Keep pure mapping/math helpers (like `SmartPresetLabeler.mapLabelToFilter`) public/internal without Android `Bitmap` dependencies so they run in fast JVM `testDebugUnitTest`.
