---
name: magnifier-camera-and-clarity
description: >-
  Guidelines and architecture for modifying camera controls, lens discovery,
  hybrid 200x zoom, Camera2 ISP tuning, and on-device super-resolution in the
  Magnifier Android app. Use when updating CameraController, CameraLensDiscovery,
  SuperResolutionEnhancer, or FocusPeakingProcessor.
---

# Magnifier Camera & Super-Resolution Clarity

## Architecture & Key Files
- `app/src/main/java/com/magnifier/app/camera/CameraLensDiscovery.kt`: Enumerates rear logical and physical cameras via `CameraManager` and classifies `Main`, `Macro`, `0.6x Wide`, and `Tele`.
- `app/src/main/java/com/magnifier/app/camera/CameraController.kt`: Wraps CameraX (`ProcessCameraProvider`, `Preview`, `ImageCapture`, `ImageAnalysis`), `Camera2Interop`, and `ExtensionsManager` (`ExtensionMode.AUTO`).
- `app/src/main/java/com/magnifier/app/processing/SuperResolutionEnhancer.kt`: Crops the zoomed ROI, upscales, performs multi-frame temporal averaging (`temporalAveragePixels`), and applies 3×3 unsharp-mask sharpening (`sharpenArgbPixels`).
- `app/src/main/java/com/magnifier/app/processing/FocusPeakingProcessor.kt`: Real-time Sobel luma edge detector for focus peaking.

## Critical Rules
1. **Never `.recycle()` Bitmaps Held by Compose `StateFlow`**:
   - Bitmaps stored in `MagnifierViewModel` (`peakingBitmap`, `frozenBitmap`) are drawn asynchronously by Compose's `RecordingCanvas` / RenderThread.
   - Calling `.recycle()` on superseded bitmaps causes `RuntimeException: Canvas: trying to use a recycled bitmap`. Let `NativeAllocationRegistry` / GC reclaim them.
   - Only call `.recycle()` on intermediate off-screen bitmaps (e.g., raw burst frames after `mergeMultiFrameAndEnhance` or temporary bitmaps after `SnapshotRepository.saveToGallery`).
2. **Always Use `syncCamera2Options` for Camera2 Capture Requests**:
   - `Camera2CameraControl.setCaptureRequestOptions(...)` replaces all previously set options.
   - Keep AF mode (`CONTROL_AF_MODE`, `LENS_FOCUS_DISTANCE`) and high-zoom ISP clarity (`EDGE_MODE_HIGH_QUALITY`, `NOISE_REDUCTION_MODE_HIGH_QUALITY` at `5×+`) unified inside `CameraController.syncCamera2Options(...)`.
3. **Hybrid 200× Zoom Split**:
   - `hardwareZoom = zoomRatio.coerceIn(minZoom, maxHardwareZoom)` is sent to `cameraControl.setZoomRatio(...)`.
   - `digitalScale = (zoomRatio / hardwareZoom).coerceAtLeast(1f)` scales the live `PreviewView` and drives `SuperResolutionEnhancer` ROI cropping.
4. **CameraX Extensions Guard**:
   - Physical-only lenses (`lens.isPhysicalOnly == true`) often do not support `ExtensionsManager` `ExtensionMode.AUTO`. Always check `extManager.isExtensionAvailable(...)` and fall back to `baseSelector`.
